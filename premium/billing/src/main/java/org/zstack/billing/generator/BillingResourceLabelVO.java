package org.zstack.billing.generator;


import javax.persistence.*;

@Table
@Entity
@IdClass(CompositePrimaryKeyForBillingResourceLabelVO.class)
public class BillingResourceLabelVO {
    @Column
    @Id
    private String resourceUuid;

    @Column
    @Id
    private String labelKey;

    @Column
    private String labelValue;

    public BillingResourceLabelVO() {
    }

    public BillingResourceLabelVO(BillingResourceLabelVO other) {
        this.resourceUuid = other.resourceUuid;
        this.labelKey = other.labelKey;
        this.labelValue = other.labelValue;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public void setLabelKey(String labelKey) {
        this.labelKey = labelKey;
    }

    public String getLabelValue() {
        return labelValue;
    }

    public void setLabelValue(String labelValue) {
        this.labelValue = labelValue;
    }
}
