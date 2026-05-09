package org.zstack.storage.encrypt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.header.core.trash.InstallPathRecycleInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Trash can keep old encrypted bits after the live resource has already moved to
 * a new install path or encryption state. Key refs must therefore survive until
 * the trash entry that still needs those bits is actually cleaned.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeEncryptedTrashCleanupHelper {
    private static final CLogger logger = Utils.getLogger(VolumeEncryptedTrashCleanupHelper.class);

    @Autowired
    private VolumeEncryptedResourceKeyBackend volumeEncryptedResourceKeyBackend;

    public void cleanupKeyRefAfterTrashDeleted(InstallPathRecycleInventory inv) {
        if (inv == null || inv.getResourceUuid() == null || inv.getResourceType() == null) {
            return;
        }

        try {
            if (VolumeVO.class.getSimpleName().equals(inv.getResourceType())) {
                cleanupVolumeKeyRef(inv.getResourceUuid());
            } else if (VolumeSnapshotVO.class.getSimpleName().equals(inv.getResourceType())) {
                cleanupSnapshotKeyRef(inv.getResourceUuid());
            }
        } catch (RuntimeException e) {
            logger.warn(String.format(
                    "failed to cleanup encrypted key ref after deleting trash[trashId:%s, resourceType:%s, resourceUuid:%s]: %s",
                    inv.getTrashId(), inv.getResourceType(), inv.getResourceUuid(), e.getMessage()));
        }
    }

    private void cleanupVolumeKeyRef(String volumeUuid) {
        Boolean encrypted = Q.New(VolumeVO.class)
                .select(VolumeVO_.encrypted)
                .eq(VolumeVO_.uuid, volumeUuid)
                .findValue();
        if (Boolean.TRUE.equals(encrypted)) {
            return;
        }

        volumeEncryptedResourceKeyBackend.detachKeyProviderFromVolume(volumeUuid);
    }

    private void cleanupSnapshotKeyRef(String snapshotUuid) {
        Boolean encrypted = Q.New(VolumeSnapshotVO.class)
                .select(VolumeSnapshotVO_.encrypted)
                .eq(VolumeSnapshotVO_.uuid, snapshotUuid)
                .findValue();
        if (Boolean.TRUE.equals(encrypted)) {
            return;
        }

        volumeEncryptedResourceKeyBackend.detachKeyProviderFromSnapshot(snapshotUuid);
    }
}
