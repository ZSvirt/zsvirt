package org.zstack.network.service.lb;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;
public class AddServerGroupToLoadBalancerListenerMsg extends NeedReplyMessage implements LoadBalancerMessage{
    private String loadBalancerUuid;

    private List<String> vmNicUuids;

    private List<String> serverIps;

    private String LoadBalancerlistenerUuid;

    private String serverGroupUuid;

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public String getLoadBalancerlistenerUuid() {
        return LoadBalancerlistenerUuid;
    }

    public void setLoadBalancerlistenerUuid(String loadBalancerlistenerUuid) {
        LoadBalancerlistenerUuid = loadBalancerlistenerUuid;
    }

    public List<String> getVmNicUuids() {
        return vmNicUuids;
    }

    public void setVmNicUuids(List<String> vmNicUuids) {
        this.vmNicUuids = vmNicUuids;
    }

    public List<String> getServerIps() {
        return serverIps;
    }

    public void setServerIps(List<String> serverIps) {
        this.serverIps = serverIps;
    }

    public String getServerGroupUuid() {
        return serverGroupUuid;
    }

    public void setServerGroupUuid(String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
    }
}
