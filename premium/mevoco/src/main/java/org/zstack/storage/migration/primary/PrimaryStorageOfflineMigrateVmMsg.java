package org.zstack.storage.migration.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.storage.migration.StorageMigrationMessage;

/**
 * Created by GuoYi on 10/14/17.
 */
public class PrimaryStorageOfflineMigrateVmMsg extends NeedReplyMessage implements StorageMigrationMessage {
    private String vmInstanceUuid;
    private String dstPrimaryStorageUuid;
    private boolean withDataVolumes;
    private boolean withSnapshots;
    private boolean discardSourceVolume = true;

    public PrimaryStorageOfflineMigrateVmMsg(PrimaryStorageMigrateVmMsg amsg) {
        this.vmInstanceUuid = amsg.getVmInstanceUuid();
        this.dstPrimaryStorageUuid = amsg.getDstPrimaryStorageUuid();
        this.withDataVolumes = amsg.isWithDataVolumes();
        this.withSnapshots = amsg.isWithSnapshots();
        this.systemTags = amsg.getSystemTags();
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

    public boolean isWithDataVolumes() {
        return withDataVolumes;
    }

    public void setWithDataVolumes(boolean withDataVolumes) {
        this.withDataVolumes = withDataVolumes;
    }

    public boolean isWithSnapshots() {
        return withSnapshots;
    }

    public void setWithSnapshots(boolean withSnapshots) {
        this.withSnapshots = withSnapshots;
    }

    public boolean isDiscardSourceVolume() {
        return discardSourceVolume;
    }

    public void setDiscardSourceVolume(boolean discardSourceVolume) {
        this.discardSourceVolume = discardSourceVolume;
    }
}
