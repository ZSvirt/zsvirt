package org.zstack.encrypt;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.encrypt.EncryptionParamParser.ParsedResult;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.encrypt.EncryptionParamAllowed;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.rest.APINoSee;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.GsonUtil;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.encrypt.EncryptSystemTags.*;

/**
 * Created by Wenhao.Zhang on 22/11/18
 */
public class EncryptionParamApiInterceptor implements GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(EncryptionParamApiInterceptor.class);
    private static final Gson gson;

    @Autowired(required = false)
    private List<EncryptionParamParser> parsers = new ArrayList<>();

    private Map<String, EncryptionParamAction> actions;
    private Map<Class<?>, Set<Field>> mergeAllowedFields = new HashMap<>();

    static {
        gson = new GsonUtil().create();
    }

    public EncryptionParamApiInterceptor() {
        actions = new HashMap<>();
        actions.put(EncryptionParamAllowed.ACTION_CHECK_USER_INFO, this::proceedCheckUserInfo);
        actions.put(EncryptionParamAllowed.ACTION_PUT_USER_INFO_INTO_SYSTEM_TAG, this::proceedPutUserInfoIntoSystemTag);
    }

    public synchronized void installParsersWithTypeIfNotExists(EncryptionParamParser... parsers) {
        Set<String> typeSet = this.parsers.stream()
                .map(EncryptionParamParser::encryptionType)
                .collect(Collectors.toSet());
        for (EncryptionParamParser parser : parsers) {
            String type = parser.encryptionType();
            if (type == null || typeSet.contains(type)) {
                continue;
            }
            typeSet.add(type);
            this.parsers.add(parser);
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        // 1. extract encryption parameters system tags. skip intercept when system tags not found.
        EncryptedMessageBundle bundle = extractEncryptParamTags(msg);
        if (bundle == null) {
            return msg;
        }

        // 2. parse encryption parameters
        logger.debug(String.format("start parsing encryption param for API message: [class=%s, encryptionType=%s]",
                msg.getClass().getSimpleName(), bundle.getEncryptionType()));
        Optional<EncryptionParamParser> parserOptional = parsers.stream()
                .filter(p -> p.encryptionType().equals(bundle.getEncryptionType()))
                .findFirst();
        if (!parserOptional.isPresent()) {
            throw new ApiMessageInterceptionException(argerr(
                    "failed to parse API message: can not parse encryption param with type %s",
                    bundle.getEncryptionType()));
        }
        EncryptionParamParser parser = parserOptional.get();
        bundle.setParser(parser);
        ErrorableValue<ParsedResult> result = parseBundle(parser, bundle);
        if (!result.isSuccess()) {
            throw new ApiMessageInterceptionException(err(
                    SysErrors.INVALID_ARGUMENT_ERROR,
                    "failed to parse API message: cipher text can not be parsed, type=%s",
                    bundle.getEncryptionType())
                    .withCause(result.error));
        }
        bundle.setUserInfo(result.result.userInfo);

        // 3. merge
        mergeApi(result.result.jsonObject, bundle);
        callExtraActions(bundle);

        return bundle.getApiMessage();
    }

    @ExceptionSafe
    private ErrorableValue<ParsedResult> parseBundle(
            EncryptionParamParser parser,
            EncryptedMessageBundle bundle) {
        return parser.parse(bundle.getCipherText(), bundle);
    }

    private EncryptedMessageBundle extractEncryptParamTags(APIMessage msg)
            throws ApiMessageInterceptionException {
        List<String> systemTags = msg.getSystemTags();
        if (CollectionUtils.isEmpty(systemTags)) {
            return null;
        }

        List<String> matchTags = systemTags.stream()
                .filter(ENCRYPTION_PARAM::isMatch)
                .collect(Collectors.toList());
        if (matchTags.isEmpty()) {
            return null;
        }

        if (matchTags.size() > 1) {
            throw new ApiMessageInterceptionException(argerr(
                    "failed to parse API message: found %d encryption param system tags, expect 1", matchTags.size()));
        }

        systemTags.removeIf(ENCRYPTION_PARAM::isMatch);

        EncryptedMessageBundle bundle = new EncryptedMessageBundle();
        bundle.setApiMessage(msg);
        bundle.setAnnotation(msg.getClass().getAnnotation(EncryptionParamAllowed.class));

        Map<String, String> tokens = ENCRYPTION_PARAM.getTokensByTag(matchTags.get(0));
        bundle.setEncryptionType(tokens.get(ENCRYPTION_TYPE_TOKEN));
        bundle.setCipherText(tokens.get(CIPHER_TEXT_TOKEN));

        return bundle;
    }

    private void mergeApi(JsonObject jsonObject, EncryptedMessageBundle bundle) {
        APIMessage apiMessage = bundle.getApiMessage();
        Set<Field> allowedFields = mergeAllowedFields.get(apiMessage.getClass());
        if (allowedFields == null) {
            throw new CloudRuntimeException(String.format(
                    "class %s encryption param allowed fields is null", apiMessage.getClass().getSimpleName()));
        }
        if (allowedFields.isEmpty()) {
            return;
        }

        Object encryptionParams = gson.fromJson(jsonObject, apiMessage.getClass());
        for (Field field : allowedFields) {
            String name = field.getName();
            if (!jsonObject.has(name)) {
                continue;
            }
            mergeField(field, encryptionParams, apiMessage);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void mergeField(Field field, Object from, Object to) {
        try {
            field.setAccessible(true);
            Object fromObject = field.get(from);
            if (fromObject == null) {
                field.set(to, null);
                return;
            }

            Object toObject = field.get(to);
            if (toObject == null) {
                field.set(to, fromObject);
                return;
            }

            // array not support
            if (fromObject instanceof Collection && toObject instanceof Collection) {
                ((Collection) field.get(to)).addAll((Collection) fromObject);
                return;
            }
            if (fromObject instanceof Map && toObject instanceof Map) {
                ((Map) field.get(to)).putAll((Map) fromObject);
                return;
            }
            field.set(to, fromObject);
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format(
                    "failed to merge field %s in API message %s", field, to.getClass().getSimpleName()));
        }
    }

    private void callExtraActions(EncryptedMessageBundle bundle) throws ApiMessageInterceptionException {
        String[] declaredActions = bundle.getAnnotation().actions();
        for (String declaredAction : declaredActions) {
            EncryptionParamAction action = actions.get(declaredAction);
            if (action == null) {
                throw new CloudRuntimeException(String.format(
                        "extra action %s for encryption param is not found", declaredAction));
            }
            action.proceed(bundle);
        }
    }

    interface EncryptionParamAction {
        void proceed(EncryptedMessageBundle bundle) throws ApiMessageInterceptionException;
    }

    public void proceedCheckUserInfo(EncryptedMessageBundle bundle) throws ApiMessageInterceptionException {
        ErrorCode errorCode = bundle.getParser().checkUserInfo(bundle.getUserInfo(), bundle);
        if (errorCode != null) {
            throw new ApiMessageInterceptionException(errorCode);
        }
    }

    public void proceedPutUserInfoIntoSystemTag(EncryptedMessageBundle bundle) throws ApiMessageInterceptionException {
        ErrorCode errorCode = bundle.getParser().putUserInfoIntoSystemTag(bundle.getUserInfo(), bundle);
        if (errorCode != null) {
            throw new ApiMessageInterceptionException(errorCode);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        // all message annotated by @EncryptionParamAllowed
        List<Class> classes = Platform.getReflections().getTypesAnnotatedWith(EncryptionParamAllowed.class)
                .stream()
                .filter(APIMessage.class::isAssignableFrom)
                .collect(Collectors.toList());

        for (Class clazz : classes) {
            mergeAllowedFields.put(clazz, buildMergeAllowedFields(clazz));
        }
        return classes;
    }

    private Set<Field> buildMergeAllowedFields(Class<?> clazz) {
        EncryptionParamAllowed annotation = clazz.getAnnotation(EncryptionParamAllowed.class);
        Set<String> forbiddenFields = new HashSet<>(Arrays.asList(annotation.forbiddenFields()));
        Map<String, Field> fieldMap = new HashMap<>();

        while (clazz != Object.class && clazz != APIMessage.class) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                fieldMap.putIfAbsent(field.getName(), field);
            }
            clazz = clazz.getSuperclass();
        }

        for (Iterator<Map.Entry<String, Field>> it = fieldMap.entrySet().iterator(); it.hasNext();) {
            Field field = it.next().getValue();
            if (field.getAnnotation(APINoSee.class) != null || forbiddenFields.contains(field.getName())) {
                it.remove();
            }
        }

        Set<Field> results = new HashSet<>(fieldMap.values());

        try {
            results.add(NeedReplyMessage.class.getDeclaredField("systemTags"));
        } catch (NoSuchFieldException e) {
            throw new CloudRuntimeException("field NeedReplyMessage.systemTags is not exists", e);
        }
        return results;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.SYSTEM;
    }

    @Override
    public int getPriority() {
        return -1;
    }
}
