package org.zstack.autoscaling.group.rule.trigger;

import org.zstack.header.vo.ResourceVO_;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Create by lining at 2018/9/22
 */
@StaticMetamodel(AutoScalingRuleTriggerVO.class)
public class AutoScalingRuleTriggerVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<AutoScalingRuleTriggerVO, String> ruleUuid;
    public static volatile SingularAttribute<AutoScalingRuleTriggerVO, String> name;
    public static volatile SingularAttribute<AutoScalingRuleTriggerVO, AutoScalingRuleTriggerType> type;
    public static volatile SingularAttribute<AutoScalingRuleTriggerVO, AutoScalingRuleTriggerState> state;
    public static volatile SingularAttribute<AutoScalingRuleTriggerVO, String> description;
    public static volatile SingularAttribute<AutoScalingRuleTriggerVO, Timestamp> createDate;
    public static volatile SingularAttribute<AutoScalingRuleTriggerVO, Timestamp> lastOpDate;
}
