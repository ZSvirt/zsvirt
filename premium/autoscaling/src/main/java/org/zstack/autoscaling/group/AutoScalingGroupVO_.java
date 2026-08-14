package org.zstack.autoscaling.group;

import org.zstack.autoscaling.group.rule.RemovalPolicy;
import org.zstack.header.vo.*;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Create by weiwang at 2018/8/16
 */
@StaticMetamodel(AutoScalingGroupVO.class)
public class AutoScalingGroupVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<AutoScalingGroupVO, String> name;
    public static volatile SingularAttribute<AutoScalingGroupVO, String> description;
    public static volatile SingularAttribute<AutoScalingGroupVO, Long> defaultCooldown;
    public static volatile SingularAttribute<AutoScalingGroupVO, ScalingResourceType> scalingResourceType;
    public static volatile SingularAttribute<AutoScalingGroupVO, RemovalPolicy> removalPolicy;
    public static volatile SingularAttribute<AutoScalingGroupVO, Integer> minResourceSize;
    public static volatile SingularAttribute<AutoScalingGroupVO, Integer> maxResourceSize;
    public static volatile SingularAttribute<AutoScalingGroupVO, AutoScalingGroupState> state;
    public static volatile SingularAttribute<AutoScalingGroupVO, Timestamp> createDate;
    public static volatile SingularAttribute<AutoScalingGroupVO, Timestamp> lastOpDate;
}
