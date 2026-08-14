package org.zstack.network.service.lb;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class AddServerIpToLoadBalancerListenerMsg extends NeedReplyMessage implements LoadBalancerMessage{
    private String loadBalancerUuid;

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

    public List<String> getServerIps() {
        return serverIps;
    }

    public void setServerIps(List<String> serverIps) {
        this.serverIps = serverIps;
    }

    public String getLoadBalancerlistenerUuid() {
        return LoadBalancerlistenerUuid;
    }

    public void setLoadBalancerlistenerUuid(String loadBalancerlistenerUuid) {
        LoadBalancerlistenerUuid = loadBalancerlistenerUuid;
    }

    public String getServerGroupUuid() {
        return serverGroupUuid;
    }

    public void setServerGroupUuid(String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
    }

}
