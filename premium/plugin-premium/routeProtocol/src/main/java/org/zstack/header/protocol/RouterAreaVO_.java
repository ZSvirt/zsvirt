package org.zstack.header.protocol;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(RouterAreaVO.class)
public class RouterAreaVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<RouterAreaVO, String> areaId;
    public static volatile SingularAttribute<RouterAreaVO, RouterAreaType> type;
    public static volatile SingularAttribute<RouterAreaVO, RouterAreaAuthType> authentication;
    public static volatile SingularAttribute<RouterAreaVO, String> password;
    public static volatile SingularAttribute<RouterAreaVO, Integer> keyId;
    public static volatile SingularAttribute<RouterAreaVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<RouterAreaVO, Timestamp> createDate;
}
