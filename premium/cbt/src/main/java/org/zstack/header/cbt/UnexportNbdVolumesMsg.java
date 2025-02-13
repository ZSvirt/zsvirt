package org.zstack.header.cbt;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class UnexportNbdVolumesMsg extends NeedReplyMessage {
    private List<String> volumeUuids;
    private String vmInstanceUuid;

    public List<String> getVolumeUuids() {
        return volumeUuids;
    }

    public void setVolumeUuids(List<String> volumeUuids) {
        this.volumeUuids = volumeUuids;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
