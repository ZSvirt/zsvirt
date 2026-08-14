package org.zstack.expon.sdk.iscsi;

import org.zstack.expon.sdk.Param;

import java.util.List;
import java.util.stream.Collectors;

public class IscsiUssResource {
    @Param
    private String serverId;
    @Param
    private String gatewayIp;

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getGatewayIp() {
        return gatewayIp;
    }

    public void setGatewayIp(String gatewayIp) {
        this.gatewayIp = gatewayIp;
    }

    public static IscsiUssResource valueOf(IscsiSeverNode node) {
        IscsiUssResource resource = new IscsiUssResource();
        resource.setServerId(node.getServerId());
        resource.setGatewayIp(node.getGatewayIp());
        return resource;
    }

    public static List<IscsiUssResource> valueOf(List<IscsiSeverNode> nodes) {
        return nodes.stream().map(IscsiUssResource::valueOf).collect(Collectors.toList());
    }
}
