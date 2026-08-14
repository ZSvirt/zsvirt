package org.zstack.compute.vm;

import org.zstack.header.message.MessageReply;

/**
 * Created by LiangHanYu on 2022/7/1 14:10
 */
public class GetVmNicQosReply extends MessageReply {
    private long outboundBandwidth = -1;
    private long inboundBandwidth = -1;

    public long getOutboundBandwidth() {
        return outboundBandwidth;
    }

    public void setOutboundBandwidth(long outboundBandwidth) {
        this.outboundBandwidth = outboundBandwidth;
    }

    public long getInboundBandwidth() {
        return inboundBandwidth;
    }

    public void setInboundBandwidth(long inboundBandwidth) {
        this.inboundBandwidth = inboundBandwidth;
    }
}
