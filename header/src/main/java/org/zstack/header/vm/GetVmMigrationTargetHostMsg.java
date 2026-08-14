package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 */
public class GetVmMigrationTargetHostMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private List<String> avoidHostUuids;

    public List<String> getAvoidHostUuids() {
        return avoidHostUuids;
    }

    public void setAvoidHostUuids(List<String> avoidHostUuids) {
        this.avoidHostUuids = avoidHostUuids;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
