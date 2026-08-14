package org.zstack.autoscaling.group.rule;

import org.zstack.header.tag.AutoDeleteTag;

import javax.persistence.*;

/**
 * Created by lining on 2018/9/4.
 */
@Entity
@Table
@AutoDeleteTag
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
public class AddingNewInstanceRuleVO extends AutoScalingRuleVO {

    @Column
    @Enumerated(EnumType.STRING)
    private AdjustmentType adjustmentType;

    @Column
    private Integer adjustmentValue;

    public AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(AdjustmentType adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public Integer getAdjustmentValue() {
        return adjustmentValue;
    }

    public void setAdjustmentValue(Integer adjustmentValue) {
        this.adjustmentValue = adjustmentValue;
    }
}
