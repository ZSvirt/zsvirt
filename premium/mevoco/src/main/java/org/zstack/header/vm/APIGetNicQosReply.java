package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 16/12/9.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetNicQosReply extends APIReply {
    private long outboundBandwidth = -1;
    private long inboundBandwidth = -1;
    private long outboundBandwidthUpthreshold = -1;
    private long inboundBandwidthUpthreshold = -1;

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
 
    public long getOutboundBandwidthUpthreshold() {
        return outboundBandwidthUpthreshold;
    }

    public void setOutboundBandwidthUpthreshold(long outboundBandwidth) {
        this.outboundBandwidthUpthreshold = outboundBandwidth;
    }

    public long getInboundBandwidthUpthreshold() {
        return inboundBandwidthUpthreshold;
    }

    public void setInboundBandwidthUpthreshold(long inboundBandwidth) {
        this.inboundBandwidthUpthreshold = inboundBandwidth;
    }

    public static APIGetNicQosReply __example__() {
        APIGetNicQosReply reply = new APIGetNicQosReply();
        reply.setInboundBandwidth(200000L);
        reply.setOutboundBandwidth(100000L);
        reply.setInboundBandwidthUpthreshold(300000L);
        reply.setOutboundBandwidthUpthreshold(300000L);
        return reply;
    }

}
