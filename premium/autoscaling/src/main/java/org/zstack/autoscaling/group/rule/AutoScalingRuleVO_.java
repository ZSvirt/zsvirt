package org.zstack.autoscaling.group.rule;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Create by weiwang at 2018/8/15
 */
@StaticMetamodel(AutoScalingRuleVO.class)
public class AutoScalingRuleVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<AutoScalingRuleVO, String> name;
    public static volatile SingularAttribute<AutoScalingRuleVO, String> type;
    public static volatile SingularAttribute<AutoScalingRuleVO, Long> cooldown;
    public static volatile SingularAttribute<AutoScalingRuleVO, String> scalingGroupUuid;
    public static volatile SingularAttribute<AutoScalingRuleVO, AutoScalingRuleState> state;
    public static volatile SingularAttribute<AutoScalingRuleVO, AutoScalingRuleStatus> status;
    public static volatile SingularAttribute<AutoScalingRuleVO, Timestamp> createDate;
    public static volatile SingularAttribute<AutoScalingRuleVO, Timestamp> lastOpDate;
}
