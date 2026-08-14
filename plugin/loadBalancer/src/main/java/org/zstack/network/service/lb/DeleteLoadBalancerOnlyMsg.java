package org.zstack.network.service.lb;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by xing5 on 2016/12/3.
 */
public class DeleteLoadBalancerOnlyMsg extends NeedReplyMessage implements LoadBalancerMessage {
    private String loadBalancerUuid;

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }
}
