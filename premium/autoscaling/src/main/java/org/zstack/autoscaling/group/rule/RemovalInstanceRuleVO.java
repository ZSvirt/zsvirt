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
public class RemovalInstanceRuleVO extends AutoScalingRuleVO {

    @Column
    @Enumerated(EnumType.STRING)
    private AdjustmentType adjustmentType;

    @Column
    private Integer adjustmentValue;

    @Column
    @Enumerated(EnumType.STRING)
    private RemovalPolicy removalPolicy;

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

    public RemovalPolicy getRemovalPolicy() {
        return removalPolicy;
    }

    public void setRemovalPolicy(RemovalPolicy removalPolicy) {
        this.removalPolicy = removalPolicy;
    }
}
