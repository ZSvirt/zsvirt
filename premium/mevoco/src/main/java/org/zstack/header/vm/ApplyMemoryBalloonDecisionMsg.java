package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;
import java.util.Map;

public class ApplyMemoryBalloonDecisionMsg extends NeedReplyMessage implements HostMessage {
    private List<String> vmInstanceUuids;
    private String direction;
    private Integer adjustPercent;
    private String hostUuid;
    private Map<String, Long> vmReservedMemory;

    public List<String> getVmInstanceUuids() {
        return vmInstanceUuids;
    }

    public void setVmInstanceUuids(List<String> vmInstanceUuids) {
        this.vmInstanceUuids = vmInstanceUuids;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Integer getAdjustPercent() {
        return adjustPercent;
    }

    public void setAdjustPercent(Integer adjustPercent) {
        this.adjustPercent = adjustPercent;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public Map<String, Long> getVmReservedMemory() {
        return vmReservedMemory;
    }

    public void setVmReservedMemory(Map<String, Long> vmReservedMemory) {
        this.vmReservedMemory = vmReservedMemory;
    }
}
