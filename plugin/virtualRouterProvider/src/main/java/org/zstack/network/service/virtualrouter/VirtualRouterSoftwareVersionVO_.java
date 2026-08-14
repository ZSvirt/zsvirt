package org.zstack.network.service.virtualrouter;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VirtualRouterSoftwareVersionVO.class)
public class VirtualRouterSoftwareVersionVO_ {
    public static volatile SingularAttribute<VirtualRouterMetadataVO, String> uuid;
    public static volatile SingularAttribute<VirtualRouterMetadataVO, String> softwareName;
    public static volatile SingularAttribute<VirtualRouterMetadataVO, String> currentVersion;
    public static volatile SingularAttribute<VirtualRouterMetadataVO, String> latestVersion;
}
