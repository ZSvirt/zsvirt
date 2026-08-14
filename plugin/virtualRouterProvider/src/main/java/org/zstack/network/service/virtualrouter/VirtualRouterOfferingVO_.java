package org.zstack.network.service.virtualrouter;

import org.zstack.header.configuration.InstanceOfferingVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VirtualRouterOfferingVO.class)
public class VirtualRouterOfferingVO_ extends InstanceOfferingVO_ {
	public static volatile SingularAttribute<VirtualRouterOfferingVO, String> managementNetworkUuid;
	public static volatile SingularAttribute<VirtualRouterOfferingVO, String> publicNetworkUuid;
	public static volatile SingularAttribute<VirtualRouterOfferingVO, String> zoneUuid;
	public static volatile SingularAttribute<VirtualRouterOfferingVO, String> imageUuid;
	public static volatile SingularAttribute<VirtualRouterOfferingVO, Boolean> isDefault;
}
