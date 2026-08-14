package org.zstack.zwatch.function;

import org.zstack.header.errorcode.OperationFailureException;

import static org.zstack.core.Platform.argerr;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ArgumentChecker {
    private String name;
    private Set<String> allowedValues;
    private Consumer<String> valueChecker;

    public ArgumentChecker(String name, String...values) {
        this.name = name;
        allowedValues = new HashSet<>();
        Collections.addAll(allowedValues, values);
    }

    public ArgumentChecker(String name, Consumer<String> valueChecker) {
        this.name = name;
        this.valueChecker = valueChecker;
    }

    public void check(String value) {
        if (allowedValues != null && !allowedValues.contains(value)) {
            throw new OperationFailureException(argerr("invalid value[%s] of the argument[%s]", value, name));
        }

        if (valueChecker != null) {
            valueChecker.accept(value);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getAllowedValues() {
        return allowedValues;
    }

    public void setAllowedValues(Set<String> allowedValues) {
        this.allowedValues = allowedValues;
    }

    public Consumer<String> getValueChecker() {
        return valueChecker;
    }

    public void setValueChecker(Consumer<String> valueChecker) {
        this.valueChecker = valueChecker;
    }
}
