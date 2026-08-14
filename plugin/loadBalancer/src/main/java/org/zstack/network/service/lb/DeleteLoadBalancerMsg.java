package org.zstack.network.service.lb;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by frank on 8/21/2015.
 */
public class DeleteLoadBalancerMsg extends NeedReplyMessage implements LoadBalancerMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getLoadBalancerUuid() {
        return uuid;
    }
}
