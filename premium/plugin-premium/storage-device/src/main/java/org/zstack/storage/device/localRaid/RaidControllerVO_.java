package org.zstack.storage.device.localRaid;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Create by weiwang at 2018/10/18
 */
@StaticMetamodel(RaidControllerVO.class)
public class RaidControllerVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<RaidControllerVO, String> name;
    public static volatile SingularAttribute<RaidControllerVO, String> description;
    public static volatile SingularAttribute<RaidControllerVO, String> productName;
    public static volatile SingularAttribute<RaidControllerVO, String> hostUuid;
    public static volatile SingularAttribute<RaidControllerVO, String> sasAddress;
    public static volatile SingularAttribute<RaidControllerVO, Timestamp> createDate;
    public static volatile SingularAttribute<RaidControllerVO, Timestamp> lastOpDate;
}
