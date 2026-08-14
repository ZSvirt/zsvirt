package org.zstack.core.validator;

@ValidateTarget(target = Length.class)
public class LengthValidator implements PropertyValidator {
    @Override
    public boolean validate(String name, String value, Object rule) throws PropertyValidatorException {
        Length length = (Length) rule;
        if (value != null && (length.min() != Long.MIN_VALUE || length.max() != Long.MAX_VALUE)) {
            int len = value.length();
            if (len < length.min() || len > length.max()) {
                throw new PropertyValidatorException("value " + value + " of property " + name + " must be in range of [ " + length.min() + " , " + length.max() + " ]");
            }
        }
        return true;
    }
}
