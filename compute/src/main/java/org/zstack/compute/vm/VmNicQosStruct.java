package org.zstack.compute.vm;

public class VmNicQosStruct {
    public String hostUuid;
    public String vmUuid;
    public String vmNicUuid;
    public String internalName;
    public Long outboundBandwidth;
    public Long inboundBandwidth;
    public Long outboundBandwidthUpthreshold;
    public Long inboundBandwidthUpthreshold;
}
