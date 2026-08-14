package org.zstack.sdk;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xing5 on 2016/12/9.
 */
public abstract class AbstractAction {
    public String apiId;
    public Long apiTimeout;

    protected abstract RestInfo getRestInfo();

    public static class Parameter {
        Field field;
        Param annotation;
    }

    // API parameter
    protected abstract Map<String, Parameter> getParameterMap();

    // Non-API parameter, likes: timeout, pollingInterval
    protected abstract Map<String, Parameter> getNonAPIParameterMap();

    private void initializeParametersIfNot() {
        synchronized (getParameterMap()) {
            if (getParameterMap().isEmpty()) {
                List<Field> fields = getAllFields();

                for (Field f : fields) {
                    Param at = f.getAnnotation(Param.class);

                    Parameter p = new Parameter();
                    p.field = f;
                    p.field.setAccessible(true);

                    NonAPIParam nonAPIParamAnnotation = f.getAnnotation(NonAPIParam.class);

                    if(at == null){
                        if(nonAPIParamAnnotation != null){
                            getNonAPIParameterMap().put(f.getName(), p);
                        }
                        continue;
                    }

                    p.annotation = at;

                    getParameterMap().put(f.getName(), p);
                }
            }
        }
    }

    protected List<Field> getAllFields() {
        Class c = getClass();
        List<Field> fs = new ArrayList<>();
        while (c != Object.class) {
            Collections.addAll(fs, c.getDeclaredFields());
            c = c.getSuperclass();
        }

        return fs;
    }

    Set<String> getAllParameterNames() {
        initializeParametersIfNot();
        return getParameterMap().keySet();
    }

    Object getParameterValue(String name){
        return getParameterValue(name, true);
    }

    Object getParameterValue(String name, boolean exceptionOnNotFound){
        return getParameterValue(getParameterMap(), name, exceptionOnNotFound);
    }

    Object getParameterValue(Map<String, Parameter> map, String name, boolean exceptionOnNotFound){
        Parameter p = map.get(name);
        if (p == null) {
            if (exceptionOnNotFound) {
                throw new ApiException(String.format("no such parameter[%s]", name));
            } else {
                return null;
            }
        }

        try {
            return p.field.get(this);
        } catch (IllegalAccessException e) {
            throw new ApiException(e);
        }
    }

    Object getNonAPIParameterValue(String name, boolean exceptionOnNotFound){
        return getParameterValue(getNonAPIParameterMap(), name, exceptionOnNotFound);
    }

    public void checkParameters() {
        initializeParametersIfNot();

        try {
            boolean sessionIdFound = false;
            boolean accessKeyIdFound = false;
            boolean accessKeySecretFound = false;
            boolean isSuppressCredentialCheck = getNonAPIParameterMap().get(Constants.IS_SUPPRESS_CREDENTIAL_CHECK) != null;
            boolean useEncryptParam = checkIfUseEncryptParam();
            for (Parameter p : getParameterMap().values()) {
                Object value = p.field.get(this);
                Param at = p.annotation;

                if (p.field.getName().equals(Constants.SESSION_ID) && value != null) {
                    sessionIdFound = true;
                }

                if (p.field.getName().equals(Constants.ACCESS_KEY_KEYID) && value != null) {
                    accessKeyIdFound = true;
                }

                if (p.field.getName().equals(Constants.ACCESS_KEY_KEY_SECRET) && value != null) {
                    accessKeySecretFound = true;
                }

                if (!useEncryptParam && at.required() && value == null) {
                    throw new ApiException(String.format("missing mandatory field[%s]", p.field.getName()));
                }

                if (value != null && (value instanceof String) && !at.noTrim()) {
                    value = ((String) value).trim();
                    p.field.set(this, value);
                }

                if (value != null && at.maxLength() != Integer.MIN_VALUE && (value instanceof String)) {
                    String str = (String) value;
                    if (str.length() > at.maxLength()) {
                        throw new ApiException(String.format("filed[%s] exceeds the max length[%s chars] of string",
                                p.field.getName(), at.maxLength()));
                    }
                }

                if (value != null && at.minLength() != 0 && (value instanceof String)) {
                    String str = (String) value;
                    if (str.length() < at.minLength()) {
                        throw new ApiException(String.format("filed[%s] less than the min length[%s chars] of string",
                                p.field.getName(), at.minLength()));
                    }
                }

                if (value != null && at.validValues().length > 0) {
                    if (value instanceof Collection) {
                        for (Object v : (Collection) value) {
                            validateValue(at.validValues(), v.toString(), p.field.getName());
                        }
                    } else {
                        validateValue(at.validValues(), value.toString(), p.field.getName());
                    }
                }

                if (value != null && at.validRegexValues() != null && at.validRegexValues().trim().equals("") == false) {
                    String regex = at.validRegexValues().trim();
                    Pattern pt = Pattern.compile(regex);
                    Matcher mt = pt.matcher(value.toString());
                    if (!mt.matches()) {
                        throw new ApiException(String.format("the value of the field[%s] doesn't match the required regular" +
                                " expression[%s]", p.field.getName(), regex));
                    }
                }

                if (value != null && at.nonempty() && value instanceof Collection) {
                    Collection col = (Collection) value;
                    if (col.isEmpty()) {
                        throw new ApiException(String.format("field[%s] cannot be an empty list", p.field.getName()));
                    }
                }

                if (value != null && !at.nullElements() && value instanceof Collection) {
                    Collection col = (Collection) value;
                    for (Object o : col) {
                        if (o == null) {
                            throw new ApiException(String.format("field[%s] cannot contain any null element", p.field.getName()));
                        }
                    }
                }

                if (value != null && !at.emptyString()) {
                    if ((value instanceof String) && ((String) value).length() == 0) {
                        throw new ApiException(String.format("the value of the field[%s] cannot be an empty string", p.field.getName()));
                    } else if (value instanceof Collection) {
                        for (Object v : (Collection) value) {
                            if (v instanceof String && ((String) v).length() == 0) {
                                throw new ApiException(String.format("the field[%s] cannot contain any empty string", p.field.getName()));
                            }
                        }
                    }
                }

                if (value != null && at.numberRange().length > 0
                        && (value instanceof Long || long.class.isAssignableFrom(value.getClass()) || value instanceof Integer || int.class.isAssignableFrom(value.getClass()))) {
                    long low = at.numberRange()[0];
                    long high = at.numberRange()[1];
                    long val = ((Number) value).longValue();
                    if (val < low || val > high) {
                        if (at.numberRangeUnit().length > 0) {
                            String lowUnit = at.numberRangeUnit()[0];
                            String highUnit = at.numberRangeUnit()[1];
                            throw new ApiException(String.format("the value of the field[%s] out of range[%s %s, %s %s]", p.field.getName(), low, lowUnit, high, highUnit));
                        } else {
                            throw new ApiException(String.format("the value of the field[%s] [%s: %s] out of range[%s, %s]", p.field.getName(), value, Long.toString(val), low, high));
                        }

                    }
                }
            }

            /* if SuppressCredentialCheck is not set, sessionId or accessKey must be provided */
            if (!isSuppressCredentialCheck && !sessionIdFound && !(accessKeyIdFound && accessKeySecretFound)) {
                throw new ApiException("sessionId or accessKey must be provided");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(e);
        }
    }

    @SuppressWarnings("rawtypes")
    private boolean checkIfUseEncryptParam() {
        Object systemTags = getParameterValue("systemTags", false);
        if (!(systemTags instanceof Collection) || ((Collection) systemTags).isEmpty()) {
            return false;
        }
        for (Object val: (Collection) systemTags) {
            if (val.toString().startsWith("encryptionParam::")) {
                return true;
            }
        }
        return false;
    }

    private static void validateValue(String[] validValues, String value, String fieldName) {
        List<String> vals = new ArrayList<>();
        for (String val: validValues) {
            vals.add(val.toLowerCase());
        }
        if (!vals.contains(value.toLowerCase())) {
            throw new ApiException(String.format("invalid value of the field[%s], valid values are %s",
                    fieldName, vals));
        }
    }
}
