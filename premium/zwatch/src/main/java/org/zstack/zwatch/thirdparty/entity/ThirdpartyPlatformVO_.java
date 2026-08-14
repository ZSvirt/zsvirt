package org.zstack.zwatch.thirdparty.entity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ThirdpartyPlatformVO.class)
public class ThirdpartyPlatformVO_ extends ResourceVO_{
    public static volatile SingularAttribute<ThirdpartyPlatformVO, String> name;
    public static volatile SingularAttribute<ThirdpartyPlatformVO, String> type;
    public static volatile SingularAttribute<ThirdpartyPlatformVO, String> url;
    public static volatile SingularAttribute<ThirdpartyPlatformVO, String> template;
    public static volatile SingularAttribute<ThirdpartyPlatformVO, String> state;
    public static volatile SingularAttribute<ThirdpartyPlatformVO, String> description;
    public static volatile SingularAttribute<ThirdpartyPlatformVO, Timestamp> lastSyncDate;
    public static volatile SingularAttribute<ThirdpartyPlatformVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<ThirdpartyPlatformVO, Timestamp> createDate;
}
