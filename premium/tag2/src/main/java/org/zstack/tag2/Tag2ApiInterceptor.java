package org.zstack.tag2;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.tag.TagPatternType;
import org.zstack.header.tag.TagPatternVO;
import org.zstack.header.tag.TagPatternVO_;
import org.zstack.identity.ResourceHelper;
import org.zstack.utils.TagUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;

@InterceptorForService("tag2")
public class Tag2ApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    private static Pattern formatPattern = Pattern.compile("^([^{}:]+::)?\\{[^{}:]+}(::\\{[^{}:]+})*$");
    private static Pattern colorPattern = Pattern.compile("^#[0-9a-fA-F]{6}$");

    @Autowired
    DatabaseFacade dbf;

    @Autowired
    CloudBus bus;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof TagPatternMessage) {
            TagPatternMessage vmsg = (TagPatternMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, Tag2Constant.SERVICE_ID, vmsg.getTagPatternUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateMessage) {
            validate((APICreateMessage) msg);

            if (msg instanceof APICreateTagMsg) {
                validate((APICreateTagMsg) msg);
            }
        } else if (msg instanceof APIUpdateTagMsg) {
            validate((APIUpdateTagMsg) msg);
        } else if (msg instanceof APIAttachTagToResourcesMsg) {
            validate((APIAttachTagToResourcesMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void validate(APICreateTagMsg msg) {
        if (msg.getColor() != null) {
            validateColor(msg.getColor());
        }
        validatePatternValue(msg.getValue(), TagPatternType.valueOf(msg.getType()));
    }

    private void validate(APIUpdateTagMsg msg) {
        if (msg.getColor() != null) {
            validateColor(msg.getColor());
        }

        if (msg.getValue() != null) {
            TagPatternVO pattern = dbf.findByUuid(msg.getTagPatternUuid(), TagPatternVO.class);
            validatePatternValue(msg.getValue(), pattern.getType());

            if (pattern.getType() == TagPatternType.withToken
                    && !TagUtils.tagPatternToSqlPattern(msg.getValue()).equals(TagUtils.tagPatternToSqlPattern(pattern.getValue()))) {
                throw new ApiMessageInterceptionException(argerr("you can only update token name"));
            } else if (pattern.getType() == TagPatternType.simple) {
                throw new ApiMessageInterceptionException(argerr("cannot update simple tag pattern format"));
            }
        }
    }

    private void validate(APIAttachTagToResourcesMsg msg) {
        TagPatternVO pattern = dbf.findByUuid(msg.getTagPatternUuid(), TagPatternVO.class);
        if (pattern.getType() == TagPatternType.withToken) {
            validateTokens(pattern.getValue(), msg.getTokens());
        } else if (msg.getTokens() != null && !msg.getTokens().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("simple tag pattern has no tokens"));
        }

        String accountUuid = ResourceHelper.findResourceOwner(pattern.getUuid());
        if (accountUuid.equals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)) {
            return;
        }

        // ensure resource owned by tag owner, even if admin permission.
        validateResourceOwner(msg.getResourceUuids(), accountUuid);
    }

    private void validate(APICreateMessage msg) throws ApiMessageInterceptionException {
        List<String> tagUuids = msg.getTagUuids();
        if (tagUuids == null || tagUuids.isEmpty()) {
            return;
        }
        for (List<String> sub : Lists.partition(tagUuids, 100)) {
            List<String> legalUuids = Q.New(TagPatternVO.class).select(TagPatternVO_.uuid)
                    .in(TagPatternVO_.uuid, sub)
                    .eq(TagPatternVO_.type, TagPatternType.simple)
                    .listValues();

            if (sub.size() != legalUuids.size()) {
                sub.removeAll(legalUuids);
                throw new ApiMessageInterceptionException(argerr("illegal tag uuids %s, tag type must be simple,", sub));
            }
        }

        // ensure resource owned by tag owner, even if admin permission.
        validateResourceOwner(tagUuids, msg.getSession().getAccountUuid());
    }

    private void validateColor(String color) {
        // for faster build, not use Color class
        if (!colorPattern.matcher(color).matches()) {
            throw new ApiMessageInterceptionException(argerr("Invalid color specification[%s], must like #FF00FF", color));
        }
    }

    private void validatePatternValue(String format, TagPatternType type) {
        if (type == TagPatternType.withToken && !formatPattern.matcher(format).matches()) {
            throw new ApiMessageInterceptionException(argerr("Get format[%s], format must like that" +
                    " name::{tokenName1}::{tokenName2} ... ::{tokenNameN} or" +
                    " {tokenName1}::{tokenName2} ... ::{tokenNameN} Name cannot contain '{}:'", format));
        }
    }

    private void validateTokens(String format, Map<String, String> tokens) {
        List<String> formatTokens = Arrays.stream(format.split("::"))
                .filter(it -> it.startsWith("{"))
                .map(it -> it.substring(1, it.length() - 1))
                .collect(Collectors.toList());
        if (!tokens.keySet().containsAll(formatTokens)) {
            throw new ApiMessageInterceptionException(argerr("all tokens %s must be specify", formatTokens));
        }
    }

    private void validateResourceOwner(List<String> resourceUuids, String expectAccountUuid) {
        for (List<String> sub : Lists.partition(resourceUuids, 100)) {
            List<String> invalidUuids = Q.New(AccountResourceRefVO.class)
                    .select(AccountResourceRefVO_.resourceUuid)
                    .in(AccountResourceRefVO_.resourceUuid, sub)
                    .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                    .notEq(AccountResourceRefVO_.accountUuid, expectAccountUuid)
                    .listValues();
            if (!invalidUuids.isEmpty()) {
                throw new ApiMessageInterceptionException(argerr("resource[uuids:%s] is not owned by account[uuid:%s]",
                        invalidUuids, expectAccountUuid));
            }
        }
    }
    @Override
    public List<Class> getMessageClassToIntercept() {
        return Collections.singletonList(APICreateMessage.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }
}
