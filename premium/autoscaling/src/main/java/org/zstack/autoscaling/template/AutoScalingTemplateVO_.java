package org.zstack.autoscaling.template;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;
import java.util.Set;

/**
 * Create by weiwang at 2018/8/16
 */
@StaticMetamodel(AutoScalingTemplateVO.class)
public class AutoScalingTemplateVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<AutoScalingTemplateVO, String> name;
    public static volatile SingularAttribute<AutoScalingTemplateVO, AutoScalingTemplateState> state;
    public static volatile SingularAttribute<AutoScalingTemplateVO, String> description;
    public static volatile SingularAttribute<AutoScalingTemplateVO, String> type;
    public static volatile SingularAttribute<AutoScalingTemplateVO, Timestamp> createDate;
    public static volatile SingularAttribute<AutoScalingTemplateVO, Timestamp> lastOpDate;
}
