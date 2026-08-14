package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;

/**
 * Created by frank on 6/8/2015.
 */
public class DeleteIsoFromPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private String vmInstanceUuid;
    private ImageSpec isoSpec;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public ImageSpec getIsoSpec() {
        return isoSpec;
    }

    public void setIsoSpec(ImageSpec isoSpec) {
        this.isoSpec = isoSpec;
    }
}
