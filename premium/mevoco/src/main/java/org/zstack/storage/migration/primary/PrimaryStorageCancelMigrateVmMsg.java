package org.zstack.storage.migration.primary;

import org.zstack.header.message.CancelMessage;
import org.zstack.storage.migration.PrimaryStorageMigrateVmMessage;

/**
 * Created by MaJin on 2019/9/9.
 */
public class PrimaryStorageCancelMigrateVmMsg extends CancelMessage implements PrimaryStorageMigrateVmMessage {
    private String vmInstanceUuid;

    private String dstPrimaryStorageUuid;

    private String dstHostUuid;

    private boolean withDataVolumes = true;

    private boolean withSnapshots = false;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getDstPrimaryStorageUuid() {
        return dstPrimaryStorageUuid;
    }

    public void setDstPrimaryStorageUuid(String dstPrimaryStorageUuid) {
        this.dstPrimaryStorageUuid = dstPrimaryStorageUuid;
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

    public String getDstHostUuid() {
        return dstHostUuid;
    }

    public void setDstHostUuid(String dstHostUuid) {
        this.dstHostUuid = dstHostUuid;
    }
}
