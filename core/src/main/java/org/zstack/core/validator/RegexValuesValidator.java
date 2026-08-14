package org.zstack.core.validator;

import java.util.regex.Pattern;

@ValidateTarget(target = RegexValues.class)
public class RegexValuesValidator implements PropertyValidator {
    @Override
    public boolean validate(String name, String value, Object rule) throws PropertyValidatorException {
        RegexValues regexValues = (RegexValues) rule;
        String regexRule = regexValues.value();
        if (value != null && !regexRule.trim().equals("")) {
            String pattern = regexRule.trim();
            boolean isMatch = Pattern.matches(pattern, value);
            if (!isMatch) {
                throw new PropertyValidatorException(String.format("invalid value [%s] for %s", value, name));
            }
        }
        return true;
    }
}
