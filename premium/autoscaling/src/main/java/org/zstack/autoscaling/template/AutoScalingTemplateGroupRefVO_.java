package org.zstack.autoscaling.template;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Create by weiwang at 2018/8/16
 */
@StaticMetamodel(AutoScalingTemplateGroupRefVO.class)
public class AutoScalingTemplateGroupRefVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<AutoScalingTemplateGroupRefVO, String> templateUuid;
    public static volatile SingularAttribute<AutoScalingTemplateGroupRefVO, String> groupUuid;
    public static volatile SingularAttribute<AutoScalingTemplateGroupRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<AutoScalingTemplateGroupRefVO, Timestamp> lastOpDate;
}
