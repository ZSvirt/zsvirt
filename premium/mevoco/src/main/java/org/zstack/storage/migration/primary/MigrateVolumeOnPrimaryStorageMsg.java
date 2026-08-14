package org.zstack.storage.migration.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.storage.migration.StorageMigrationMessage;

/**
 * Created by GuoYi on 10/14/17.
 */
public class MigrateVolumeOnPrimaryStorageMsg extends NeedReplyMessage implements StorageMigrationMessage {
    private String volumeUuid;
    private String srcPrimaryStorageUuid;
    private String dstPrimaryStorageUuid;
    private String vmInstanceUuid;
    private String type;
    private boolean discardSourceVolume = true;
    private boolean migrateWithSnapshots = true;

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

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isDiscardSourceVolume() {
        return discardSourceVolume;
    }

    public void setDiscardSourceVolume(boolean discardSourceVolume) {
        this.discardSourceVolume = discardSourceVolume;
    }

    public boolean isMigrateWithSnapshots() {
        return migrateWithSnapshots;
    }

    public void setMigrateWithSnapshots(boolean migrateWithSnapshots) {
        this.migrateWithSnapshots = migrateWithSnapshots;
    }
}
