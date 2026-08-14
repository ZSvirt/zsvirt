package org.zstack.utils.verify;

import org.zstack.utils.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class ParamValidator {
    private static Set<Class<? extends Verifiable>> verifiableClasses = BeanUtils.reflections.getSubTypesOf(Verifiable.class);
    private static Map<Class<? extends Verifiable>, Map<Field, Param>> verifiableParams = verifiableClasses.stream()
            .collect(Collectors.toMap(clz -> clz,
                    clz -> FieldUtils.getAnnotatedFields(Param.class, clz).stream()
                            .collect(Collectors.toMap(field -> field, field -> field.getAnnotation(Param.class)))
            ));

    public static void validate(Verifiable verifiable) throws IllegalAccessException {
        List<String> errors = new ArrayList<>();
        for (Map.Entry<Field, Param> entry : verifiableParams.get(verifiable.getClass()).entrySet()) {
            Field f = entry.getKey();
            Param param = entry.getValue();

            f.setAccessible(true);
            Object value = f.get(verifiable);
            if (param.required() && value == null) {
                errors.add(String.format("field[%s] cannot be null.", f.getName()));
            }

            if (value != null && value instanceof String && !param.noTrim()) {
                f.set(verifiable, ((String) value).trim());
            }

            if (value != null && param.numberRange().length > 0 && TypeUtils.isTypeOf(value, Integer.TYPE, Integer.class, Long.TYPE, Long.class)) {
                DebugUtils.Assert(param.numberRange().length == 2, String.format("invalid field[%s], Param.numberRange must have and only have 2 items.", f.getName()));
                long low = param.numberRange()[0];
                long high = param.numberRange()[1];
                long val = ((Number) value).longValue();
                if (val < low || val > high) {
                    errors.add(String.format("field[%s] must be in range of [%s, %s].", f.getName(), low, high));
                }
            }

            if (value != null && param.maxLength() != Integer.MAX_VALUE && (value instanceof String)) {
                String str = (String) value;
                if (str.length() > param.maxLength()) {
                    errors.add(String.format("field[%s] of message[%s] exceeds max length of string. expected was <= %s, actual was %s.",
                            f.getName(), verifiable.getClass().getName(), param.maxLength(), str.length()));
                }
            }

            if (value != null && param.minLength() != 0 && (value instanceof String)) {
                String str = (String) value;
                if (str.length() < param.minLength()) {
                    errors.add(String.format("field[%s] of message[%s] less than the min length of string. expected was >= %s, actual was %s.",
                            f.getName(), verifiable.getClass().getName(), param.minLength(), str.length()));
                }
            }

            if (value != null && value instanceof String && param.resourceType() != Object.class && !StringDSL.isZStackUuid(((String) value).trim())) {
                errors.add(String.format("field[%s] is not a valid uuid.", f.getName()));
            }

            if (value != null && param.validValues().length > 0) {
                boolean found = false;
                for (String val : param.validValues()) {
                    if (val.equals(value.toString())) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    errors.add(String.format("valid value for field[%s] of message[%s] are %s, but %s found",
                            f.getName(), verifiable.getClass().getName(), asList(param.validValues()), value));
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", errors));
        }
    }
}
