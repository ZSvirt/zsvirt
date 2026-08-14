package org.zstack.sdk;



public class RemovalInstanceRuleInventory extends org.zstack.sdk.AutoScalingRuleInventory {

    public java.lang.String removalPolicy;
    public void setRemovalPolicy(java.lang.String removalPolicy) {
        this.removalPolicy = removalPolicy;
    }
    public java.lang.String getRemovalPolicy() {
        return this.removalPolicy;
    }

    public java.lang.String adjustmentType;
    public void setAdjustmentType(java.lang.String adjustmentType) {
        this.adjustmentType = adjustmentType;
    }
    public java.lang.String getAdjustmentType() {
        return this.adjustmentType;
    }

    public java.lang.Integer adjustmentValue;
    public void setAdjustmentValue(java.lang.Integer adjustmentValue) {
        this.adjustmentValue = adjustmentValue;
    }
    public java.lang.Integer getAdjustmentValue() {
        return this.adjustmentValue;
    }

}
