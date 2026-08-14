package org.zstack.network.service.virtualrouter;

import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.APICreateVmInstanceMsg;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

/*
@RestRequest(
		path = "/vm-instances/appliances/virtual-routers",
		method = HttpMethod.POST,
		responseClass = APICreateVmInstanceEvent.class,
		parameterName = "params"
)
*/
public class APICreateVirtualRouterVmMsg extends APICreateVmInstanceMsg {
	@APIParam(resourceType = L3NetworkVO.class)
    private String managementNetworkUuid;
	@APIParam(resourceType = L3NetworkVO.class)
    private String publicNetworkUuid;
	@APIParam
	private Set<String> networkServicesProvided;
	
	public String getManagementNetworkUuid() {
		return managementNetworkUuid;
	}
	public void setManagementNetworkUuid(String managementNetworkUuid) {
		this.managementNetworkUuid = managementNetworkUuid;
	}
	public String getPublicNetworkUuid() {
		return publicNetworkUuid;
	}
	public void setPublicNetworkUuid(String publicNetworkUuid) {
		this.publicNetworkUuid = publicNetworkUuid;
	}
	public Set<String> getNetworkServicesProvided() {
		return networkServicesProvided;
	}
	public void setNetworkServicesProvided(Set<String> networkServicesProvided) {
		this.networkServicesProvided = networkServicesProvided;
	}
	
 
    public static APICreateVirtualRouterVmMsg __example__() {
        APICreateVirtualRouterVmMsg msg = new APICreateVirtualRouterVmMsg();

		msg.setName("Test-Router");
		msg.setDescription("this is a virtual router vm");
		msg.setClusterUuid(uuid());
		msg.setImageUuid(uuid());
		msg.setInstanceOfferingUuid(uuid());
        msg.setManagementNetworkUuid(uuid());
        msg.setPublicNetworkUuid(uuid());
        Set<String> s = new HashSet<>();
        s.add(NetworkServiceType.DHCP.toString());
        msg.setNetworkServicesProvided(s);
        msg.setL3NetworkUuids(asList(uuid(),uuid()));

        return msg;
    }

}
