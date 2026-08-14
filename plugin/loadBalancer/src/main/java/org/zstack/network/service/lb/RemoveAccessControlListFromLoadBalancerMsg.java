package org.zstack.network.service.lb;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class RemoveAccessControlListFromLoadBalancerMsg extends NeedReplyMessage implements LoadBalancerMessage{
    private List<String> aclUuids;
    private String loadBalancerUuid;
    private String listenerUuid;
    private List<String> serverGroupUuids;

    public List<String> getAclUuids() {
        return aclUuids;
    }

    public void setAclUuids(List<String> aclUuids) {
        this.aclUuids = aclUuids;
    }

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
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
