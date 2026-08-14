package org.zstack.core.validator;

import java.util.Arrays;

@ValidateTarget(target = AvailableValues.class)
public class AvailableValuesValidator implements PropertyValidator {
    @Override
    public boolean validate(String name, String value, Object rule) throws PropertyValidatorException {
        AvailableValues availableValues = (AvailableValues) rule;
        String[] validValues = availableValues.value();
        if (value != null && validValues.length > 0) {
            if (Arrays.stream(validValues).noneMatch(it -> it.equals(value))) {
                throw new PropertyValidatorException(String.format("valid value of property [%s] are %s , actually found '%s'",
                        name, Arrays.toString(validValues), value));
            }
        }
        return true;
    }
}
