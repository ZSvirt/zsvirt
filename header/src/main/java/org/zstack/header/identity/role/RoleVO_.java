package org.zstack.header.identity.role;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(RoleVO.class)
public class RoleVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<RoleVO, String> name;
    public static volatile SingularAttribute<RoleVO, String> description;
    public static volatile SingularAttribute<RoleVO, RoleType> type;
    public static volatile SingularAttribute<RoleVO, Timestamp> createDate;
    public static volatile SingularAttribute<RoleVO, Timestamp> lastOpDate;
}
