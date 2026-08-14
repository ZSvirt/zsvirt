package org.zstack.zwatch.datatype;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.rest.SDK;
import org.zstack.utils.gson.JSONObjectUtil;
import static org.zstack.core.Platform.argerr;

@SDK
public class Label {
    public enum Operator {
        // the order does mater, don't change it, see public Label(String str);
        Regex("=~"),
        Filter(":="),
        NotEqual("!="),
        RegexAgainst("!~"),
        Equal("=");

        Operator(String op) {
            this.op = op;
        }

        private final String op;

        @Override
        public String toString() {
            return op;
        }

        public static Operator fromString(String str) {
            for (Operator operator : Operator.values()) {
                if (operator.op.equals(str)) {
                    return operator;
                }
            }

            throw new CloudRuntimeException(String.format("unknown operator[%s]", str));
        }
    }

    private String key;
    private String value;
    private Operator op = Operator.Equal;
    private boolean compatible = false;


    public Label(String str) {
        op = null;

        for (Operator o : Operator.values()) {
            if (str.contains(o.toString())) {
                op = o;
                break;
            }
        }

        if (op == null) {
            throw new OperationFailureException(argerr("the label string[%s] contains no valid operator", str));
        }

        String[] pairs = str.split(op.toString(), 2);
        key = pairs[0].trim();
        value = pairs[1].trim();
    }

    public Label(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public Label(String key, Operator op, String value) {
        this.op = op;
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

    public boolean match(String v) {
        if (op == Operator.Equal && value.isEmpty() && v == null) {
            // an empty value means "not contain the label"
            return true;
        }

        if (v == null) {
            return false;
        }

        if (op == Operator.Equal) {
            return value.equals(v);
        } else if (op == Operator.Regex) {
            return v.matches(value);
        } else if (op == Operator.NotEqual) {
            return value.equals(v);
        } else {
            throw new CloudRuntimeException(String.format("unknown operator[%s]", op));
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

    public Operator getOp() {
        return op;
    }

    public void setOp(Operator op) {
        this.op = op;
    }

    @Override
    public String toString() {
        return String.format("%s%s%s", key, op, value);
    }

    public boolean isCompatible() {
        return compatible;
    }

    public void setCompatible(boolean compatible) {
        this.compatible = compatible;
    }
}
