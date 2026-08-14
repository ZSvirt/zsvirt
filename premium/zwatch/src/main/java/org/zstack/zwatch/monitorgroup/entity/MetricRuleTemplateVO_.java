package org.zstack.zwatch.monitorgroup.entity;

import org.zstack.header.vo.ResourceVO_;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(MetricRuleTemplateVO.class)
public class MetricRuleTemplateVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<MetricRuleTemplateVO, String> name;
    public static volatile SingularAttribute<MetricRuleTemplateVO, String> monitorTemplateUuid;
    public static volatile SingularAttribute<MetricRuleTemplateVO, ComparisonOperator> comparisonOperator;
    public static volatile SingularAttribute<MetricRuleTemplateVO, Integer> period;
    public static volatile SingularAttribute<MetricRuleTemplateVO, Integer> repeatInterval;
    public static volatile SingularAttribute<MetricRuleTemplateVO, Integer> repeatCount;
    public static volatile SingularAttribute<MetricRuleTemplateVO, String> namespace;
    public static volatile SingularAttribute<MetricRuleTemplateVO, String> metricName;
    public static volatile SingularAttribute<MetricRuleTemplateVO, EmergencyLevel> emergencyLevel;
    public static volatile SingularAttribute<MetricRuleTemplateVO, String> labels;
    public static volatile SingularAttribute<MetricRuleTemplateVO, Timestamp> createDate;
    public static volatile SingularAttribute<MetricRuleTemplateVO, Timestamp> lastOpDate;
}
