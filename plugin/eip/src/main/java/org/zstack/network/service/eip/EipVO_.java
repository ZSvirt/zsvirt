package org.zstack.network.service.eip;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 */
@StaticMetamodel(EipVO.class)
public class EipVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<EipVO, String> name;
    public static volatile SingularAttribute<EipVO, String> description;
    public static volatile SingularAttribute<EipVO, EipState> state;
    public static volatile SingularAttribute<EipVO, String> vmNicUuid;
    public static volatile SingularAttribute<EipVO, String> vipUuid;
    public static volatile SingularAttribute<EipVO, String> vipIp;
    public static volatile SingularAttribute<EipVO, String> guestIp;
    public static volatile SingularAttribute<EipVO, Timestamp> createDate;
    public static volatile SingularAttribute<EipVO, Timestamp> lastOpDate;
}
