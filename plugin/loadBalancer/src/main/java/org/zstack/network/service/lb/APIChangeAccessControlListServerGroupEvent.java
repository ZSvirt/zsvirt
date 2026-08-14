package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventory")
public class APIChangeAccessControlListServerGroupEvent extends APIEvent {
    private LoadBalancerListerAcl inventory;

    public APIChangeAccessControlListServerGroupEvent() {
    }

    public APIChangeAccessControlListServerGroupEvent(String apiId) {
        super(apiId);
    }

    public LoadBalancerListerAcl getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerListerAcl inventory) {
        this.inventory = inventory;
    }

    public static APIChangeLoadBalancerBackendServerEvent __example__() {
        APIChangeLoadBalancerBackendServerEvent event = new APIChangeLoadBalancerBackendServerEvent();
        LoadBalancerServerGroupInventory loadBalancerServerGroupIv = new LoadBalancerServerGroupInventory();
        loadBalancerServerGroupIv.setName("servergroup");
        loadBalancerServerGroupIv.setUuid(uuid());
        loadBalancerServerGroupIv.setLoadBalancerUuid(uuid());
        return event;
    }

    static class LoadBalancerListerAcl {
        private String aclUuid;
        private String listenerUuid;
        private List<String> serverGroupUuids;

        public String getAclUuid() {
            return aclUuid;
        }

        public void setAclUuid(String aclUuid) {
            this.aclUuid = aclUuid;
        }

        public String getListenerUuid() {
            return listenerUuid;
        }

        public void setListenerUuid(String listenerUuid) {
            this.listenerUuid = listenerUuid;
        }

        public List<String> getServerGroupUuids() {
            return serverGroupUuids;
        }

        public void setServerGroupUuids(List<String> serverGroupUuids) {
            this.serverGroupUuids = serverGroupUuids;
        }
    }
}
