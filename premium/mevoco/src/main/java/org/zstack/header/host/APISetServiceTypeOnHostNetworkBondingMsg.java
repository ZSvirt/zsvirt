package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;

import java.util.Collections;
import java.util.List;

@RestRequest(
        path = "/hosts/bondings/service-types",
        method = HttpMethod.POST,
        responseClass = APISetServiceTypeOnHostNetworkBondingEvent.class,
        parameterName = "params"
)
public class APISetServiceTypeOnHostNetworkBondingMsg extends APIMessage {
    /**
     * @desc uuids of bondings which are going to set service type
     */
    @APIParam(resourceType = HostNetworkBondingVO.class)
    private List<String> bondingUuids;

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

    public List<String> getBondingUuids() {
        return bondingUuids;
    }

    public void setBondingUuids(List<String> bondingUuids) {
        this.bondingUuids = bondingUuids;
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

    public void setServiceTypes(List<String> serviceTypes) {
        this.serviceTypes = serviceTypes;
    }

    public static APISetServiceTypeOnHostNetworkBondingMsg __example__() {
        APISetServiceTypeOnHostNetworkBondingMsg msg = new APISetServiceTypeOnHostNetworkBondingMsg();
        msg.setBondingUuids(Collections.singletonList((uuid())));
        msg.setServiceTypes(Collections.singletonList("TenantNetwork"));
        return msg;
    }
}
