package org.zstack.storage.migration.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.storage.migration.StorageMigrationMessage;

/**
 * Created by GuoYi on 12/7/17.
 *
 * This message is for PrimaryStorageMigrateVolumeJob
 */

public class PrimaryStorageMigrateVolumeMsg extends NeedReplyMessage implements StorageMigrationMessage {
    private String volumeUuid;
    private String srcPrimaryStorageUuid;
    private String dstPrimaryStorageUuid;
    private String type;
    private String vmInstanceUuid;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getSrcPrimaryStorageUuid() {
        return srcPrimaryStorageUuid;
    }

    public void setSrcPrimaryStorageUuid(String srcPrimaryStorageUuid) {
        this.srcPrimaryStorageUuid = srcPrimaryStorageUuid;
    }

    public String getDstPrimaryStorageUuid() {
        return dstPrimaryStorageUuid;
    }

    public void setDstPrimaryStorageUuid(String dstPrimaryStorageUuid) {
        this.dstPrimaryStorageUuid = dstPrimaryStorageUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
