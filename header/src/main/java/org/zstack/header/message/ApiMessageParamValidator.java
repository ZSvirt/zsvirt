package org.zstack.header.message;

import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.TypeUtils;
import org.zstack.header.message.APIMessage.InvalidApiMessageException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiMessageParamValidator implements ApiMessageValidator, Ordered {
    
    @Override
    public void validate(APIMessage msg, Field f, Object value, APIParam at) {
        if (at.required() && value == null) {
            throw new InvalidApiMessageException("field[%s] of message[%s] is mandatory, can not be null", f.getName(), msg.getClass().getName());
        }
        
        if (value != null) {
            validateNonNullValue(msg, f, value, at);
        }
    }
    
    private void validateNonNullValue(APIMessage msg, Field f, Object value, APIParam at) {
        if (at.maxLength() != Integer.MIN_VALUE && (value instanceof String)) {
            String str = (String) value;
            if (str.length() > at.maxLength()) {
                throw new InvalidApiMessageException("field[%s] of message[%s] exceeds max length of string. expected was <= %s, actual was %s",
                    f.getName(), msg.getClass().getName(), at.maxLength(), str.length());
            }
        }
    
        if (at.minLength() != 0 && (value instanceof String)) {
            String str = (String) value;
            if (str.length() < at.minLength()) {
                throw new InvalidApiMessageException("field[%s] of message[%s] less than the min length of string. expected was >= %s, actual was %s",
                    f.getName(), msg.getClass().getName(), at.minLength(), str.length());
            }
        }
    
        if (at.validValues().length > 0) {
            Collection<?> values = (value instanceof Collection) ?
                    (Collection<?>) value : Collections.singletonList(value);
            for (Object v : values) {
                validateValue(at.validValues(), v.toString(), f.getName(), msg.getClass().getName());
            }
        } else if (at.validEnums().length > 0) {
            Collection<?> values = (value instanceof Collection) ?
                    (Collection<?>) value : Collections.singletonList(value);
            final String[] validValues = CollectionUtils.valuesForEnums(at.validEnums()).toArray(String[]::new);
            for (Object v : values) {
                validateValue(validValues, v.toString(), f.getName(), msg.getClass().getName());
            }
        }
    
        if (at.validRegexValues().trim().equals("") == false) {
            String regex = at.validRegexValues().trim();
            Pattern p = Pattern.compile(regex);
            Matcher mt = p.matcher(value.toString());
            if (!mt.matches()){
                throw new InvalidApiMessageException("valid regex value for field[%s] of message[%s] are %s, but %s found", f.getName(),
                    msg.getClass().getName(), regex, value);
            }
        }
    
        if (at.nonempty() && value instanceof Collection) {
            Collection<?> col = (Collection<?>) value;
            if (col.isEmpty()) {
                throw new InvalidApiMessageException("field[%s] must be a nonempty list", f.getName());
            }
        }
    
        if (!at.nullElements() && value instanceof Collection) {
            Collection<?> col = (Collection<?>) value;
            for (Object o : col) {
                if (o == null) {
                    throw new InvalidApiMessageException("field[%s] cannot contain a NULL element", f.getName());
                }
            }
        }
    
        if (!at.emptyString()) {
            if (value instanceof String && StringUtils.isEmpty(value)) {
                throw new InvalidApiMessageException("field[%s] cannot be an empty string", f.getName());
            } else if (value instanceof Collection) {
                for (Object v : (Collection<?>) value) {
                    if (v instanceof String && StringUtils.isEmpty(v)) {
                        throw new InvalidApiMessageException("field[%s] cannot contain any empty string", f.getName());
                    }
                }
            }
        }
    
        if (at.numberRange().length > 0 && TypeUtils.isTypeOf(value, Integer.TYPE, Integer.class, Long.TYPE, Long.class)) {
            DebugUtils.Assert(at.numberRange().length == 2, String.format("invalid field[%s], APIParam.numberRange must have and only have 2 items", f.getName()));
            long low = at.numberRange()[0];
            long high = at.numberRange()[1];
            long val = ((Number) value).longValue();
            if (val < low || val > high) {
                if (at.numberRangeUnit().length > 0) {
                    DebugUtils.Assert(at.numberRangeUnit().length == 2, String.format("invalid field[%s], APIParam.numberRangeUnit must have and only have 2 items", f.getName()));
                    String lowUnit = at.numberRangeUnit()[0];
                    String highUnit = at.numberRangeUnit()[1];
                    throw new InvalidApiMessageException("field[%s] must be in range of [%s %s, %s %s]", f.getName(), low, lowUnit, high, highUnit);
                } else {
                    throw new InvalidApiMessageException("field[%s] must be in range of [%s, %s]", f.getName(), low, high);
                }
            }
        }
    }
    
    private void validateValue(String[] validValues, String value, String fieldName, String msgName) {
        if (Arrays.stream(validValues).noneMatch(it -> it.equals(value))) {
            throw new InvalidApiMessageException("valid value for field[%s] of message[%s] are %s, but %s found",
                fieldName, msgName, Arrays.toString(validValues), value);
        }
    }
    
    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
