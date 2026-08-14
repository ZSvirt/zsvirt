package org.zstack.network.service.virtualrouter;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VirtualRouterMetadataVO.class)
public class VirtualRouterMetadataVO_ {
    public static volatile SingularAttribute<VirtualRouterMetadataVO, String> uuid;
    public static volatile SingularAttribute<VirtualRouterMetadataVO, String> zvrVersion;
    public static volatile SingularAttribute<VirtualRouterMetadataVO, String> vyosVersion;
    public static volatile SingularAttribute<VirtualRouterMetadataVO, String> kernelVersion;
}
