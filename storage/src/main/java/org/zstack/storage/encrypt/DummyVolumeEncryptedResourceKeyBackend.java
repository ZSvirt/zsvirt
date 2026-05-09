package org.zstack.storage.encrypt;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * OSS / no-premium-crypto: no-op volume key-provider persistence, same role as
 * {@link org.zstack.compute.vm.devices.DummyTpmEncryptedResourceKeyBackend}.
 */
public class DummyVolumeEncryptedResourceKeyBackend implements VolumeEncryptedResourceKeyBackend {
    private static final CLogger logger = Utils.getLogger(DummyVolumeEncryptedResourceKeyBackend.class);

    @Override
    public void attachKeyProviderToVolume(String volumeUuid, String keyProviderUuid) {
        logger.debug(String.format("ignore attach key provider to volume[uuid:%s] keyProviderUuid:%s",
                volumeUuid, keyProviderUuid));
    }

    @Override
    public void detachKeyProviderFromVolume(String volumeUuid) {
        logger.debug(String.format("ignore detach key provider from volume[uuid:%s]", volumeUuid));
    }

    @Override
    public void detachKeyProviderFromSnapshot(String snapshotUuid) {
        logger.debug(String.format("ignore detach key provider from snapshot[uuid:%s]", snapshotUuid));
    }

    @Override
    public void detachKeyProviderFromTemporarySnapshotImage(String imageUuid) {
        logger.debug(String.format("ignore detach key provider from temporary snapshot image[uuid:%s]", imageUuid));
    }

    @Override
    public String findKeyProviderUuidByVolume(String volumeUuid) {
        return null;
    }

    @Override
    public boolean checkVolumeKeyProviderAttached(String volumeUuid) {
        return false;
    }

    @Override
    public boolean checkSnapshotKeyProviderAttached(String snapshotUuid) {
        return false;
    }

    @Override
    public boolean checkTemporarySnapshotImageKeyProviderAttached(String imageUuid) {
        return false;
    }

    @Override
    public void copyVolumeKeyToSnapshot(String volumeUuid, String snapshotUuid) {
        logger.debug(String.format("ignore copy volume[uuid:%s] key to snapshot[uuid:%s]", volumeUuid, snapshotUuid));
    }

    @Override
    public void copySnapshotKeyToVolume(String snapshotUuid, String volumeUuid) {
        logger.debug(String.format("ignore copy snapshot[uuid:%s] key to volume[uuid:%s]", snapshotUuid, volumeUuid));
    }

    @Override
    public void copyVolumeKeyToVolume(String srcVolumeUuid, String dstVolumeUuid) {
        logger.debug(String.format("ignore copy volume[uuid:%s] key to volume[uuid:%s]", srcVolumeUuid, dstVolumeUuid));
    }

    @Override
    public void copySnapshotKeyToTemporarySnapshotImage(String snapshotUuid, String imageUuid) {
        logger.debug(String.format("ignore copy snapshot[uuid:%s] key to temporary snapshot image[uuid:%s]", snapshotUuid, imageUuid));
    }

    @Override
    public void copyTemporarySnapshotImageKeyToVolume(String imageUuid, String volumeUuid) {
        logger.debug(String.format("ignore copy temporary snapshot image[uuid:%s] key to volume[uuid:%s]", imageUuid, volumeUuid));
    }

    @Override
    public String defaultKeyProviderUuid() {
        return null;
    }

    @Override
    public String findKeyProviderUuidBySnapshot(String snapshotUuid) {
        return null;
    }

    @Override
    public String findKeyProviderUuidByTemporarySnapshotImage(String imageUuid) {
        return null;
    }

    @Override
    public Integer findKeyVersionByVolume(String volumeUuid) {
        return null;
    }

    @Override
    public Integer findKeyVersionBySnapshot(String snapshotUuid) {
        return null;
    }
}
