package org.zstack.guesttools;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 2019-09-19.
 */
@StaticMetamodel(GuestToolsVO.class)
public class GuestToolsVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<GuestToolsVO, String> name;
    public static volatile SingularAttribute<GuestToolsVO, String> description;
    public static volatile SingularAttribute<GuestToolsVO, String> managementNodeUuid;
    public static volatile SingularAttribute<GuestToolsVO, String> architecture;
    public static volatile SingularAttribute<GuestToolsVO, String> hypervisorType;
    public static volatile SingularAttribute<GuestToolsVO, String> version;
    public static volatile SingularAttribute<GuestToolsVO, String> agentType;
    public static SingularAttribute<GuestToolsVO, Timestamp> createDate;
    public static SingularAttribute<GuestToolsVO, Timestamp> lastOpDate;
}
