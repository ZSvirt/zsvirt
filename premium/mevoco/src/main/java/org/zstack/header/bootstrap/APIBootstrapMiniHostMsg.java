package org.zstack.header.bootstrap;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.*;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

@TagResourceType(ClusterVO.class)
@RestRequest(
        path = "/mini-clusters/hosts",
        parameterName = "params",
        method = HttpMethod.POST,
        responseClass = APIBootstrapMiniHostEvent.class
)
public class APIBootstrapMiniHostMsg extends APICreateMessage {
    @APIParam
    private MiniHostInfo local;

    @APIParam
    private MiniHostInfo peer;

    public MiniHostInfo getLocal() {
        return local;
    }

    public void setLocal(MiniHostInfo local) {
        this.local = local;
    }

    public MiniHostInfo getPeer() {
        return peer;
    }

    public void setPeer(MiniHostInfo peer) {
        this.peer = peer;
    }

    public static APIBootstrapMiniHostMsg __example__() {
        APIBootstrapMiniHostMsg msg = new APIBootstrapMiniHostMsg();

        MiniNetworkConfigStruct localIpmi = new MiniNetworkConfigStruct();
        localIpmi.setGw("172.20.27.1");
        localIpmi.setIp("172.20.27.21/24");
        localIpmi.setVlan("27");

        MiniNetworkConfigStruct localMgmt = new MiniNetworkConfigStruct();
        localMgmt.setVlan("26");
        localMgmt.setIp("172.20.26.21/24");
        localMgmt.setGw("172.20.26.1");
        localMgmt.setBond("lacp");

        MiniNetworkConfigStruct peerMgmt = new MiniNetworkConfigStruct();
        peerMgmt.setVlan("26");
        peerMgmt.setIp("172.20.26.22/24");
        peerMgmt.setGw("172.20.26.1");
        peerMgmt.setBond("ab");

        MiniHostInfo local = new MiniHostInfo();
        local.setSn("RD-BJD-001-07311A");
        local.setIpmi(localIpmi);
        local.setMgmt(localMgmt);

        MiniHostInfo peer = new MiniHostInfo();
        peer.setSn("RD-BJD-001-07311B");
        peer.setMgmt(peerMgmt);

        msg.setLocal(local);
        msg.setPeer(peer);
        return msg;
    }
}
