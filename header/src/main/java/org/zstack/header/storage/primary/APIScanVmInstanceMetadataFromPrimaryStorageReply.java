package org.zstack.header.storage.primary;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIScanVmInstanceMetadataFromPrimaryStorageReply extends APIReply {
    private List<VmMetadataScanEntry> vmInstanceMetadata = new ArrayList<>();

    public List<VmMetadataScanEntry> getVmInstanceMetadata() {
        return vmInstanceMetadata;
    }

    public void setVmInstanceMetadata(List<VmMetadataScanEntry> vmInstanceMetadata) {
        this.vmInstanceMetadata = vmInstanceMetadata == null ? new ArrayList<>() : vmInstanceMetadata;
    }

    public static APIScanVmInstanceMetadataFromPrimaryStorageReply __example__() {
        APIScanVmInstanceMetadataFromPrimaryStorageReply evt = new APIScanVmInstanceMetadataFromPrimaryStorageReply();
        return evt;
    }
}
