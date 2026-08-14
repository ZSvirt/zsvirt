package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

/**
 * Created by mingjian.deng on 16/10/19.
 */
public class GetNicQosOnKVMHostReply extends MessageReply {
    private Long inbound = -1L;
    private Long outbound = -1L;

    public Long getInbound() {
        return inbound;
    }

    public void setInbound(Long inbound) {
        this.inbound = inbound;
    }

    public Long getOutbound() {
        return outbound;
    }

    public void setOutbound(Long outbound) {
        this.outbound = outbound;
    }
}
