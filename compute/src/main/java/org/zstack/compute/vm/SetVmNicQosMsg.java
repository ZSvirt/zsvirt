package org.zstack.compute.vm;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceMessage;

/**
 * Created by LiangHanYu on 2022/7/1 15:32
 */
public class SetVmNicQosMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String uuid;
    private Long outboundBandwidth;
    private Long inboundBandwidth;
    private String vmInstanceUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Long getOutboundBandwidth() {
        return outboundBandwidth;
    }

    public void setOutboundBandwidth(Long outboundBandwidth) {
        this.outboundBandwidth = outboundBandwidth;
    }

    public Long getInboundBandwidth() {
        return inboundBandwidth;
    }

    public void setInboundBandwidth(Long inboundBandwidth) {
        this.inboundBandwidth = inboundBandwidth;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
