package org.zstack.zwatch.datatype;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.gson.JSONObjectUtil;

import static org.zstack.core.Platform.argerr;

public class ValueCondition {
    public enum Key {
        value
    }

    public enum Operator {
        // the order does mater, don't change it, see public ValueCondition(String str);
        Equal("=="),
        NotEqual("!="),
        GreaterThan(">"),
        LessThan("<"),
        GreaterOrEqual(">="),
        LessOrEqual("<=");

        Operator(String op) {
            this.op = op;
        }

        private final String op;

        @Override
        public String toString() {
            return op;
        }

        public static ValueCondition.Operator fromString(String str) {
            for (ValueCondition.Operator operator : ValueCondition.Operator.values()) {
                if (operator.op.equals(str)) {
                    return operator;
                }
            }

            throw new CloudRuntimeException(String.format("unknown operator[%s]", str));
        }
    }

    private String key;
    private String value;
    private ValueCondition.Operator op = ValueCondition.Operator.Equal;

    public ValueCondition(String str) {
        op = null;

        int max = 0;
        for (ValueCondition.Operator o : ValueCondition.Operator.values()) {
            if (str.contains(o.toString())) {
                int currentOperatorLength = o.toString().length();
                if (currentOperatorLength <= max) {
                    continue;
                }

                max = currentOperatorLength;
                op = o;
            }
        }

        if (op == null) {
            throw new OperationFailureException(argerr("the label string[%s] contains no valid operator", str));
        }

        String[] pairs = str.split(op.toString(), 2);
        key = pairs[0].trim();

        if (!Key.value.toString().equals(key)) {
            throw new OperationFailureException(argerr("the ValueCondition string[%s] require 'value' as key ", str));
        }

        value = pairs[1].trim();
    }

    public ValueCondition(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public void check() {
        if (key == null) {
            throw new OperationFailureException(argerr("invalid label, 'key' field cannot be null. %s", JSONObjectUtil.toJsonString(this)));
        }
        if (op == null) {
            throw new OperationFailureException(argerr("invalid label, 'op' field is null or something another than Regex and Equal. %s", JSONObjectUtil.toJsonString(this)));
        }
        if (value == null) {
            throw new OperationFailureException(argerr("invalid label, 'value' field cannot be null. %s", JSONObjectUtil.toJsonString(this)));
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ValueCondition.Operator getOp() {
        return op;
    }

    public void setOp(ValueCondition.Operator op) {
        this.op = op;
    }

    @Override
    public String toString() {
        return String.format("%s%s%s", key, op, value);
    }
}
