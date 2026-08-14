package org.zstack.storage.primary.imagestore.smp;

import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.SQL;
import org.zstack.header.message.Message;
import org.zstack.header.storage.primary.*;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageConstant;
import org.zstack.storage.primary.smp.*;
import org.zstack.header.storage.primary.ResizeVolumeOnPrimaryStorageMsg;
import org.zstack.storage.backup.imagestore.CleanImageMetaOnPrimaryStorageMsg;
import org.zstack.storage.primary.smp.HypervisorBackend;
import org.zstack.storage.primary.smp.HypervisorFactory;
import org.zstack.storage.primary.smp.SMPPrimaryStorageBase;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;

/**
 * Created by david on 7/27/16.
 */
public class SMPImageStoreBackend extends SMPPrimaryStorageBase {
    private PrimaryStorageVO self;

    public SMPImageStoreBackend(PrimaryStorageVO ps) {
        self = ps;
    }

    @Transactional(readOnly = true)
    private String getClusterUuidByVolumeUuid(String volumeUuid){
        return SQL.New("select vm.clusterUuid from VmInstanceVO vm, VolumeVO vol"
                + " where vol.uuid = :volumeUuid and vol.vmInstanceUuid = vm.uuid", String.class)
                .param("volumeUuid", volumeUuid)
                .find();
    }

    private HypervisorBackend getHypervisorBackendFromVolumeUuid(String volumeUuid) {
        String clusterUuid = getClusterUuidByVolumeUuid(volumeUuid);
        String hvType = getHypervisorTypeByClusterUuid(clusterUuid);
        HypervisorFactory factory = getHypervisorFactoryByHypervisorAndExtensionType(hvType,
                ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE);
        return factory.getHypervisorBackend(self);
    }

    private void handle(final CommitVolumeAsImageOnPrimaryStorageMsg msg) {
        HypervisorBackend bkd = getHypervisorBackendFromVolumeUuid(msg.getVolumeUuid());
        bkd.handleLocalMessage(msg);
    }

    private void handle(final CommitVolumeAsImageMsg msg) {
        HypervisorBackend bkd = getHypervisorBackendFromVolumeUuid(msg.getVolumeUuid());
        bkd.handleLocalMessage(msg);
    }

    private void handle(final SelectBackupStorageMsg msg) {
        HypervisorBackend bkd = getHypervisorBackendFromVolumeUuid(msg.getVolumeUuid());
        bkd.handleLocalMessage(msg);
    }

    private void handle(final CleanImageMetaOnPrimaryStorageMsg msg) {
        HypervisorBackend bkd = getHypervisorBackendFromVolumeUuid(msg.getVolumeUuid());
        bkd.handleLocalMessage(msg);
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof CommitVolumeAsImageMsg) {
            handle((CommitVolumeAsImageMsg) msg);
        } else if (msg instanceof SelectBackupStorageMsg) {
            handle((SelectBackupStorageMsg) msg);
        } else if (msg instanceof CommitVolumeAsImageOnPrimaryStorageMsg) {
            handle((CommitVolumeAsImageOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CleanImageMetaOnPrimaryStorageMsg) {
            handle((CleanImageMetaOnPrimaryStorageMsg) msg);
        }
    }
}
