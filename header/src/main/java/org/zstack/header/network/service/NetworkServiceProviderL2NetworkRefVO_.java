package org.zstack.header.network.service;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(NetworkServiceProviderL2NetworkRefVO.class)
public class NetworkServiceProviderL2NetworkRefVO_ {
    public static volatile SingularAttribute<NetworkServiceProviderL2NetworkRefVO, Long> id;
    public static volatile SingularAttribute<NetworkServiceProviderL2NetworkRefVO, String> networkServiceProviderUuid;
    public static volatile SingularAttribute<NetworkServiceProviderL2NetworkRefVO, String> l2NetworkUuid;
}
