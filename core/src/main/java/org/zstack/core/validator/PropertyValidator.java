package org.zstack.core.validator;

public interface PropertyValidator {
    boolean validate(String name, String value, Object rule) throws PropertyValidatorException;
}
