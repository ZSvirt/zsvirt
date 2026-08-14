package org.zstack.autoscaling.group.activity;

import org.zstack.header.vo.ResourceVO_;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Create by lining at 2018/9/14
 */
@StaticMetamodel(AutoScalingGroupActivityVO.class)
public class AutoScalingGroupActivityVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<AutoScalingGroupActivityVO, String> name;
    public static volatile SingularAttribute<AutoScalingGroupActivityVO, String> scalingGroupUuid;
    public static volatile SingularAttribute<AutoScalingGroupActivityVO, AutoScalingGroupActivityAction> activityAction;
    public static volatile SingularAttribute<AutoScalingGroupActivityVO, String> instanceUuids;
    public static volatile SingularAttribute<AutoScalingGroupActivityVO, String> scalingGroupRuleUuid;
    public static volatile SingularAttribute<AutoScalingGroupActivityVO, AutoScalingGroupActivityCause> cause;
    public static volatile SingularAttribute<AutoScalingGroupActivityVO, String> description;
    public static volatile SingularAttribute<AutoScalingGroupActivityVO, AutoScalingGroupActivityStatus> status;
    public static volatile SingularAttribute<AutoScalingGroupActivityVO, String> activityActionResultMessage;
    public static volatile SingularAttribute<AutoScalingGroupActivityVO, Timestamp> createDate;
    public static volatile SingularAttribute<AutoScalingGroupActivityVO, Timestamp> endDate;
    public static volatile SingularAttribute<AutoScalingGroupActivityVO, Timestamp> lastOpDate;
}
