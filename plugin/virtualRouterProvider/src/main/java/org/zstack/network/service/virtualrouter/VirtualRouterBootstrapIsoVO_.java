package org.zstack.network.service.virtualrouter;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VirtualRouterBootstrapIsoVO.class)
public class VirtualRouterBootstrapIsoVO_ {
	public static volatile SingularAttribute<VirtualRouterBootstrapIsoVO, Long> id;
	public static volatile SingularAttribute<VirtualRouterBootstrapIsoVO, String> virtualRouterUuid;
	public static volatile SingularAttribute<VirtualRouterBootstrapIsoVO, String> isoPath;
}
