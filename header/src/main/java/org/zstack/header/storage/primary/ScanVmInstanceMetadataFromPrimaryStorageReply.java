package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

import java.util.ArrayList;
import java.util.List;

public class ScanVmInstanceMetadataFromPrimaryStorageReply extends MessageReply {
    private List<VmMetadataScanEntry> vmInstanceMetadata = new ArrayList<>();

    public List<VmMetadataScanEntry> getVmInstanceMetadata() {
        return vmInstanceMetadata;
    }

    public void setVmInstanceMetadata(List<VmMetadataScanEntry> vmInstanceMetadata) {
        this.vmInstanceMetadata = vmInstanceMetadata == null ? new ArrayList<>() : new ArrayList<>(vmInstanceMetadata);
    }
}
