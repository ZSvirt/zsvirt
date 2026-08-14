package org.zstack.network.service.lb;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @author: zhanyong.miao
 * @date: 2019-12-26
 **/
public class LoadBalancerGetPeerL3NetworksMsg extends NeedReplyMessage implements LoadBalancerMessage {
    private String loadBalancerUuid;

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }
}