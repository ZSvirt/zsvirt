package org.zstack.sdk.zwatch.datatype;

import org.zstack.sdk.zwatch.datatype.Operator;

public class Label  {

    public java.lang.String key;
    public void setKey(java.lang.String key) {
        this.key = key;
    }
    public java.lang.String getKey() {
        return this.key;
    }

    public java.lang.String value;
    public void setValue(java.lang.String value) {
        this.value = value;
    }
    public java.lang.String getValue() {
        return this.value;
    }

    public Operator op;
    public void setOp(Operator op) {
        this.op = op;
    }
    public Operator getOp() {
        return this.op;
    }

    public boolean compatible;
    public void setCompatible(boolean compatible) {
        this.compatible = compatible;
    }
    public boolean getCompatible() {
        return this.compatible;
    }

}
