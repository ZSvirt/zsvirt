package org.zstack.network.service.lb;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by frank on 8/14/2015.
 */
public class LoadBalancerRemoveVmNicMsg extends NeedReplyMessage implements LoadBalancerMessage {
    private String loadBalancerUuid;
    private List<String> listenerUuids;
    private List<String> serverGroupUuids;
    private List<String> vmNicUuids;

    public List<String> getListenerUuids() {
        return listenerUuids;
    }

    public void setListenerUuids(List<String> listenerUuids) {
        this.listenerUuids = listenerUuids;
    }

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public List<String> getVmNicUuids() {
        return vmNicUuids;
    }

    public void setVmNicUuids(List<String> vmNicUuids) {
        this.vmNicUuids = vmNicUuids;
    }

    public List<String> getServerGroupUuids() {
        return serverGroupUuids;
    }

    public void setServerGroupUuids(List<String> serverGroupUuids) {
        this.serverGroupUuids = serverGroupUuids;
    }
}
