package org.zstack.sdk.zwatch.alarm;

import org.zstack.sdk.zwatch.datatype.Operator;

public class EventSubscriptionLabelInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String key;
    public void setKey(java.lang.String key) {
        this.key = key;
    }
    public java.lang.String getKey() {
        return this.key;
    }

    public Operator operator;
    public void setOperator(Operator operator) {
        this.operator = operator;
    }
    public Operator getOperator() {
        return this.operator;
    }

    public java.lang.String value;
    public void setValue(java.lang.String value) {
        this.value = value;
    }
    public java.lang.String getValue() {
        return this.value;
    }

}
