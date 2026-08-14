package org.zstack.autoscaling.group.rule;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Create by lining at 2018/9/11
 */
@StaticMetamodel(AddingNewInstanceRuleVO.class)
public class AddingNewInstanceRuleVO_ extends AutoScalingRuleVO_ {
    public static volatile SingularAttribute<AddingNewInstanceRuleVO, AdjustmentType> adjustmentType;
    public static volatile SingularAttribute<AddingNewInstanceRuleVO, Integer> adjustmentValue;
}
