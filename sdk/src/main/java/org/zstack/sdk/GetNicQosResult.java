package org.zstack.sdk;



public class GetNicQosResult {
    public long outboundBandwidth;
    public void setOutboundBandwidth(long outboundBandwidth) {
        this.outboundBandwidth = outboundBandwidth;
    }
    public long getOutboundBandwidth() {
        return this.outboundBandwidth;
    }

    public long inboundBandwidth;
    public void setInboundBandwidth(long inboundBandwidth) {
        this.inboundBandwidth = inboundBandwidth;
    }
    public long getInboundBandwidth() {
        return this.inboundBandwidth;
    }

    public long outboundBandwidthUpthreshold;
    public void setOutboundBandwidthUpthreshold(long outboundBandwidthUpthreshold) {
        this.outboundBandwidthUpthreshold = outboundBandwidthUpthreshold;
    }
    public long getOutboundBandwidthUpthreshold() {
        return this.outboundBandwidthUpthreshold;
    }

    public long inboundBandwidthUpthreshold;
    public void setInboundBandwidthUpthreshold(long inboundBandwidthUpthreshold) {
        this.inboundBandwidthUpthreshold = inboundBandwidthUpthreshold;
    }
    public long getInboundBandwidthUpthreshold() {
        return this.inboundBandwidthUpthreshold;
    }

}
