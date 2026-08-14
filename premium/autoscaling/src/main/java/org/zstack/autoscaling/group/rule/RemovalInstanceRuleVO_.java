package org.zstack.autoscaling.group.rule;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Create by lining at 2018/9/12
 */
@StaticMetamodel(RemovalInstanceRuleVO.class)
public class RemovalInstanceRuleVO_ extends AutoScalingRuleVO_ {
    public static volatile SingularAttribute<AutoScalingRuleVO, AdjustmentType> adjustmentType;
    public static volatile SingularAttribute<AutoScalingRuleVO, RemovalPolicy> removalPolicy;
    public static volatile SingularAttribute<AutoScalingRuleVO, Integer> adjustmentValue;
}
