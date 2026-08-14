package org.zstack.core.validator;

@ValidateTarget(target = NumberRange.class)
public class NumberRangeValidator implements PropertyValidator {
    @Override
    public boolean validate(String name, String value, Object rule) throws PropertyValidatorException {
        NumberRange numberRange = (NumberRange) rule;
        if (value != null && (numberRange.min() != Long.MIN_VALUE || numberRange.max() != Long.MAX_VALUE)) {
            long val = Long.parseLong(value);
            if (val < numberRange.min() || val > numberRange.max()) {
                throw new PropertyValidatorException("value " + val + " of property" + name + " must be in range of [ " + numberRange.min() + " , " + numberRange.max() + " ]");
            }
        }
        return true;
    }
}
