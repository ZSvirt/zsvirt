package org.zstack.autoscaling.group.instance;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Create by lining at 2018/9/14
 */
@StaticMetamodel(AutoScalingGroupInstanceVO.class)
public class AutoScalingGroupInstanceVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<AutoScalingGroupInstanceVO, String> instanceUuid;
    public static volatile SingularAttribute<AutoScalingGroupInstanceVO, String> scalingGroupUuid;
    public static volatile SingularAttribute<AutoScalingGroupInstanceVO, String> templateUuid;
    public static volatile SingularAttribute<AutoScalingGroupInstanceVO, String> scalingGroupActivityUuid;
    public static volatile SingularAttribute<AutoScalingGroupInstanceVO, AutoScalingGroupInstanceStatus> status;
    public static volatile SingularAttribute<AutoScalingGroupInstanceVO, AutoScalingGroupInstanceHealthStatus> healthStatus;
    public static volatile SingularAttribute<AutoScalingGroupInstanceVO, String> description;
    public static volatile SingularAttribute<AutoScalingGroupInstanceVO, Timestamp> createDate;
    public static volatile SingularAttribute<AutoScalingGroupInstanceVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<AutoScalingGroupInstanceVO, String> protectionStrategy;
}
