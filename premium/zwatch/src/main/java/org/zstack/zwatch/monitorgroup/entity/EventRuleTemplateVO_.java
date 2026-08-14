package org.zstack.zwatch.monitorgroup.entity;

import org.zstack.header.vo.ResourceVO_;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(EventRuleTemplateVO.class)
public class EventRuleTemplateVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<EventRuleTemplateVO, String> name;
    public static volatile SingularAttribute<EventRuleTemplateVO, String> monitorTemplateUuid;
    public static volatile SingularAttribute<EventRuleTemplateVO, ComparisonOperator> comparisonOperator;
    public static volatile SingularAttribute<EventRuleTemplateVO, String> namespace;
    public static volatile SingularAttribute<EventRuleTemplateVO, String> eventName;
    public static volatile SingularAttribute<EventRuleTemplateVO, EmergencyLevel> emergencyLevel;
    public static volatile SingularAttribute<EventRuleTemplateVO, String> labels;
    public static volatile SingularAttribute<EventRuleTemplateVO, Timestamp> createDate;
    public static volatile SingularAttribute<EventRuleTemplateVO, Timestamp> lastOpDate;
}
