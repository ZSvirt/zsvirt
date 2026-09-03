package org.zstack.loginControl.entity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(AccessControlRuleVO.class)
public class AccessControlRuleVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<AccessControlRuleVO, String> rule;
    public static volatile SingularAttribute<AccessControlRuleVO, ControlStrategy> strategy;
    public static volatile SingularAttribute<AccessControlRuleVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<AccessControlRuleVO, Timestamp> createDate;
}
