package org.zstack.storage.primary.imagestore.local;

import org.zstack.header.message.Message;
import org.zstack.header.storage.primary.*;
import org.zstack.storage.backup.imagestore.CleanImageMetaOnPrimaryStorageMsg;
import org.zstack.storage.primary.local.LocalStorageBase;
import org.zstack.storage.primary.local.LocalStorageHypervisorBackend;

/**
 * Created by david on 7/27/16.
 */
public class LocalStorageImageStoreBackend extends LocalStorageBase {
    public LocalStorageImageStoreBackend(PrimaryStorageVO self) {
        super(self);
    }

    public LocalStorageImageStoreBackend() {
    }

    @Override
    public void handleLocalMessage(Message msg) {
        if (msg instanceof CommitVolumeAsImageMsg) {
            handle((CommitVolumeAsImageMsg) msg);
        } else if (msg instanceof SelectBackupStorageMsg) {
            handle((SelectBackupStorageMsg) msg);
        } else if (msg instanceof CommitVolumeAsImageOnPrimaryStorageMsg) {
            handle((CommitVolumeAsImageOnPrimaryStorageMsg) msg);
        } else if (msg instanceof ResizeVolumeOnPrimaryStorageMsg) {
            handle((ResizeVolumeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CleanImageMetaOnPrimaryStorageMsg) {
            handle((CleanImageMetaOnPrimaryStorageMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    private void handle(final CleanImageMetaOnPrimaryStorageMsg msg) {
        String hostUuid = getHostUuidByResourceUuid(msg.getVolumeUuid());
        LocalStorageHypervisorBackend bkd = getHypervisorBackendFactoryByHostUuid(hostUuid).getHypervisorBackend(self);
        bkd.handleLocalMessage(msg);
    }


    private void handle(final ResizeVolumeOnPrimaryStorageMsg msg) {
        String hostUuid = getHostUuidByResourceUuid(msg.getVolume().getUuid());
        LocalStorageHypervisorBackend bkd = getHypervisorBackendFactoryByHostUuid(hostUuid).getHypervisorBackend(self);
        bkd.handleLocalMessage(msg);
    }

    private void handle(final SelectBackupStorageMsg msg) {
        String hostUuid = getHostUuidByResourceUuid(msg.getVolumeUuid());
        LocalStorageHypervisorBackend bkd = getHypervisorBackendFactoryByHostUuid(hostUuid).getHypervisorBackend(self);
        bkd.handleLocalMessage(msg);
    }

    private void handle(final CommitVolumeAsImageMsg msg) {
        String hostUuid = getHostUuidByResourceUuid(msg.getVolumeUuid());
        LocalStorageHypervisorBackend bkd = getHypervisorBackendFactoryByHostUuid(hostUuid).getHypervisorBackend(self);
        bkd.handleLocalMessage(msg);
    }

    private void handle(final CommitVolumeAsImageOnPrimaryStorageMsg msg) {
        String hostUuid = getHostUuidByResourceUuid(msg.getVolumeUuid());
        LocalStorageHypervisorBackend bkd = getHypervisorBackendFactoryByHostUuid(hostUuid).getHypervisorBackend(self);
        bkd.handleLocalMessage(msg);
    }
}
