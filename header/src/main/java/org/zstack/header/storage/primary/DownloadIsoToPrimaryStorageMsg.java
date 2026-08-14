package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.message.ReplayableMessage;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;

/**
 * Created by frank on 5/23/2015.
 */
public class DownloadIsoToPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage, ReplayableMessage {
    private String primaryStorageUuid;
    private ImageSpec isoSpec;
    private String vmInstanceUuid;
    private String destHostUuid;

    public String getDestHostUuid() {
        return destHostUuid;
    }

    public void setDestHostUuid(String destHostUuid) {
        this.destHostUuid = destHostUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public ImageSpec getIsoSpec() {
        return isoSpec;
    }

    public void setIsoSpec(ImageSpec isoSpec) {
        this.isoSpec = isoSpec;
    }

    @Override
    public String getResourceUuid() {
        return vmInstanceUuid;
    }

    @Override
    public Class getReplayableClass() {
        return DownloadIsoToPrimaryStorageMsg.class;
    }
}
