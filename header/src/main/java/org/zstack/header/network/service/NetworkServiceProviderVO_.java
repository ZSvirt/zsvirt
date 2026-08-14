package org.zstack.header.network.service;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(NetworkServiceProviderVO.class)
public class NetworkServiceProviderVO_ {
    public static volatile SingularAttribute<NetworkServiceProviderVO, String> uuid;
    public static volatile SingularAttribute<NetworkServiceProviderVO, String> name;
    public static volatile SingularAttribute<NetworkServiceProviderVO, String> type;
    public static volatile SingularAttribute<NetworkServiceProviderVO, String> description;
    public static volatile SingularAttribute<NetworkServiceProviderVO, Timestamp> createDate;
    public static volatile SingularAttribute<NetworkServiceProviderVO, Timestamp> lastOpDate;
}
