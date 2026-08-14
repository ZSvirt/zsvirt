package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;

import java.util.Collections;
import java.util.List;

@RestRequest(
        path = "/hosts/nics/service-types",
        method = HttpMethod.POST,
        responseClass = APISetServiceTypeOnHostNetworkInterfaceEvent.class,
        parameterName = "params"
)
public class APISetServiceTypeOnHostNetworkInterfaceMsg extends APIMessage {
    /**
     * @desc uuids of interfaces which are going to set service type
     */
    @APIParam(resourceType = HostNetworkInterfaceVO.class)
    private List<String> interfaceUuids;

    /**
     * @desc the ids of vlan interfaces of the interface which are going to set service type
     */
    @APIParam(required = false)
    private List<Integer> vlanIds;

    /**
     * @desc max length of 128 characters
     */
    @APIParam(required = false, validValues = {"ManagementNetwork", "TenantNetwork", "StorageNetwork", "BackupNetwork", "MigrationNetwork"}, maxLength = 128)
    private List<String> serviceTypes;

    public List<String> getInterfaceUuids() {
        return interfaceUuids;
    }

    public void setInterfaceUuids(List<String> interfaceUuids) {
        this.interfaceUuids = interfaceUuids;
    }

    public List<Integer> getVlanIds() {
        return vlanIds;
    }

    public void setVlanIds(List<Integer> vlanIds) {
        this.vlanIds = vlanIds;
    }

    public List<String> getServiceTypes() {
        return serviceTypes;
    }

    public void setServiceTypes(List<String> serviceType) {
        this.serviceTypes = serviceTypes;
    }

    public static APISetServiceTypeOnHostNetworkInterfaceMsg __example__() {
        APISetServiceTypeOnHostNetworkInterfaceMsg msg = new APISetServiceTypeOnHostNetworkInterfaceMsg();
        msg.setInterfaceUuids(Collections.singletonList((uuid())));
        msg.setServiceTypes(Collections.singletonList("TenantNetwork"));
        return msg;
    }
}
