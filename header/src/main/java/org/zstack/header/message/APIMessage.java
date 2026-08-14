package org.zstack.header.message;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public abstract class APIMessage extends NeedReplyMessage implements ConfigurableTimeoutMessage, APIMessageDefinition {
    /**
     * @ignore
     */
    @NoJsonSchema
    @APINoSee
    private SessionInventory session;
    @APINoSee
    private String clientIp;
    @APINoSee
    private String clientBrowser;

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }

    public static class FieldParam {
        public Field field;
        public APIParam param;
    }

    @NoJsonSchema
    @APINoSee
    @GsonTransient
    public static Map<Class, Collection<FieldParam>> apiParams = new HashMap<>();

    @NoJsonSchema
    @APINoSee
    @GsonTransient
    public static Set<Class> apiMessageClasses = BeanUtils.reflections.getSubTypesOf(APIMessage.class)
            .stream().filter(c -> !Modifier.isStatic(c.getModifiers()) && c.isAnnotationPresent(RestRequest.class))
            .collect(Collectors.toSet());

    static {
        collectApiParams();
        collectResponseMappingFields();
    }

    private static void collectResponseMappingFields() {
        BeanUtils.reflections.getSubTypesOf(APIResponse.class).stream().filter(clz-> clz != APIReply.class && clz != APIEvent.class
         && !Modifier.isStatic(clz.getModifiers()) && clz.isAnnotationPresent(RestResponse.class)).forEach(clz -> {
            RestResponse annotation = clz.getAnnotation(RestResponse.class);
            Map responseMappingFields = new HashMap();

            if (!annotation.allTo().equals("")) {
                responseMappingFields.put(annotation.allTo(), annotation.allTo());
            } else if (annotation.fieldsTo().length > 0) {
                if (annotation.fieldsTo().length == 1 && "all".equals(annotation.fieldsTo()[0])) {
                    List<Field> apiFields = FieldUtils.getAllFields(clz);
                    apiFields = apiFields.stream().filter(f -> !f.isAnnotationPresent(APINoSee.class) && !Modifier.isStatic(f.getModifiers())).collect(Collectors.toList());

                    for (Field f : apiFields) {
                        responseMappingFields.put(f.getName(), f.getName());
                    }
                } else {
                    for (String mf : annotation.fieldsTo()) {
                        String[] kv = mf.split("=");
                        if (kv.length == 2) {
                            responseMappingFields.put(kv[0].trim(), kv[1].trim());
                        } else if (kv.length == 1) {
                            responseMappingFields.put(kv[0].trim(), kv[0].trim());
                        } else {
                            throw new CloudRuntimeException(String.format("bad mappingFields[%s] of %s", mf, clz));
                        }

                    }
                }
            }

            APIResponse.responseMappingFields.put(clz, responseMappingFields);
        });
    }

    public static Map<Class, Collection<FieldParam>> getApiParams() {
        return apiParams;
    }

    private static void collectApiParams() {
        class Nothing { @APIParam(required = false) public String nothing;}
        APIParam defaultAnnotation;
        try {
            defaultAnnotation = Nothing.class.getField("nothing").getAnnotation(APIParam.class);
        } catch (NoSuchFieldException e) {
            throw new CloudRuntimeException(e);
        }

        for (Class<?> clz : apiMessageClasses) {
            List<Field> fields = FieldUtils.getAllFields(clz);

            Map<String, FieldParam> fmap = new HashMap<>();
            for (Field f : fields) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                if (f.isAnnotationPresent(APINoSee.class)) {
                    continue;
                }
                if (Modifier.isTransient(f.getModifiers())) {
                    continue;
                }
                if (f.isAnnotationPresent(GsonTransient.class)) {
                    continue;
                }

                APIParam at = f.getAnnotation(APIParam.class);
                if (at == null) {
                    at = defaultAnnotation;
                }

                f.setAccessible(true);
                FieldParam fp = new FieldParam();
                fp.field = f;
                fp.param = at;
                fmap.put(f.getName(), fp);
            }

            OverriddenApiParams at = (OverriddenApiParams) clz.getAnnotation(OverriddenApiParams.class);
            if (at != null) {
                for (OverriddenApiParam atp : at.value()) {
                    Field f = FieldUtils.getField(atp.field(), clz);
                    if (f == null) {
                        throw new CloudRuntimeException(String.format("cannot find the field[%s] specified in @OverriddenApiParam of class[%s]",
                                atp.field(), clz));
                    }

                    FieldParam param = fmap.get(atp.field());
                    param.param = atp.param();
                }
            }

            apiParams.put(clz, fmap.values());
        }
    }

    public static class InvalidApiMessageException extends RuntimeException {
        private Object[] arguments = new Object[]{};

        public InvalidApiMessageException(Object[] arguments) {
            this.arguments = arguments;
        }

        public InvalidApiMessageException(String message, Object...arguments) {
            super(message);
            this.arguments = arguments;
        }

        public InvalidApiMessageException(String message, Throwable cause, Object[] arguments) {
            super(message, cause);
            this.arguments = arguments;
        }

        public InvalidApiMessageException(Throwable cause, Object[] arguments) {
            super(cause);
            this.arguments = arguments;
        }

        public InvalidApiMessageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Object[] arguments) {
            super(message, cause, enableSuppression, writableStackTrace);
            this.arguments = arguments;
        }

        public Object[] getArguments() {
            return arguments;
        }

        public void setArguments(Object[] arguments) {
            this.arguments = arguments;
        }
    }


    public void validate() throws IllegalAccessException {
        validate(Collections.emptyList());
    }

    public void validate(Collection<ApiMessageValidator> validators) throws IllegalAccessException {
        Collection<FieldParam> params = apiParams.get(this.getClass());
        if (params == null) {
            throw new CloudRuntimeException(String.format("cannot find ApiParams for the class[%s]", this.getClass()));
        }

        for (FieldParam fp : params) {
            Field f = fp.field;
            final APIParam at = fp.param;

            f.setAccessible(true);
            Object value = f.get(this);

            if (value instanceof String && !at.noTrim()) {
                value = ((String) value).trim();
                f.set(this, value);
            }
            
            for (ApiMessageValidator validator : validators) {
                validator.validate(this, f, value, at);
            }
        }
    }

    private static void validateValue(String[] validValues, String value, String fieldName, String msgName) {
        if (Arrays.stream(validValues).noneMatch(it -> it.equals(value))) {
            throw new InvalidApiMessageException("valid value for field[%s] of message[%s] are %s, but %s found",
                    fieldName, msgName, Arrays.toString(validValues), value);
        }
    }

    public static boolean isReadOnlyApi(Class<?> apiClass) {
        // TODO: will add RestRequest.readOnly()
        // Note: APIGenerateSshKeyPairMsg is not a read-only api, but it is a sync api
        if (apiClass.getSimpleName().equals("APIGenerateSshKeyPairMsg")
                || apiClass.getSimpleName().equals("APIBatchSyncVolumeSizeMsg")
                || apiClass.getSimpleName().equals("APICheckElaborationContentMsg")) {
            return false;
        }
        return APISyncCallMessage.class.isAssignableFrom(apiClass);
    }

    public String getOperator() {
        return null;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getClientBrowser() {
        return clientBrowser;
    }

    public void setClientBrowser(String clientBrowser) {
        this.clientBrowser = clientBrowser;
    }
}
