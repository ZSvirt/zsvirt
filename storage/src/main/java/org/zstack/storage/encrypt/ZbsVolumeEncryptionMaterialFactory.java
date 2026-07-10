package org.zstack.storage.encrypt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.addon.primary.CreateVolumeSpec;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.volume.VolumeManager;

import static org.zstack.core.Platform.operr;

public class ZbsVolumeEncryptionMaterialFactory {
    @Autowired
    private VolumeEncryptedSecretHelper volumeEncryptedSecretHelper;
    @Autowired
    private VolumeSnapshotEncryptionHelper snapshotEncryptionHelper;
    @Autowired
    private VolumeManager volumeManager;

    ZbsVolumeEncryptionMaterial prepareVolumeEncryption(String primaryStorageUuid, String volumeUuid, String operation) {
        if (StringUtils.isBlank(volumeUuid)) {
            throw new OperationFailureException(operr("prepare encrypted ZBS volume requires target volume uuid"));
        }

        String hostUuid = findConnectedKvmHost(primaryStorageUuid, operation);
        String encryptedDek = volumeEncryptedSecretHelper.materializeAndSealVolumeDekForHost(hostUuid, volumeUuid);
        if (StringUtils.isBlank(encryptedDek)) {
            throw new OperationFailureException(operr(
                    "cannot prepare LUKS encryptedDek for encrypted volume[uuid:%s] on host[uuid:%s]",
                    volumeUuid, hostUuid));
        }
        return new ZbsVolumeEncryptionMaterial(hostUuid, encryptedDek);
    }

    ZbsVolumeEncryptionMaterial prepareSnapshotCopyEncryption(String primaryStorageUuid, String srcInstallPath,
                                                              CreateVolumeSpec dst) {
        if (StringUtils.isBlank(dst.getUuid())) {
            throw new OperationFailureException(operr("cannot copy encrypted ZBS volume from snapshot, target uuid is missing"));
        }

        boolean targetVolume = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, dst.getUuid()).isExists();
        if (targetVolume) {
            return prepareVolumeEncryption(primaryStorageUuid, dst.getUuid(),
                    String.format("copy encrypted volume[uuid:%s] from snapshot", dst.getUuid()));
        }

        String snapshotUuid = Q.New(VolumeSnapshotVO.class)
                .select(VolumeSnapshotVO_.uuid)
                .eq(VolumeSnapshotVO_.primaryStorageUuid, primaryStorageUuid)
                .eq(VolumeSnapshotVO_.primaryStorageInstallPath, srcInstallPath)
                .findValue();
        if (StringUtils.isBlank(snapshotUuid)) {
            throw new OperationFailureException(operr(
                    "cannot find source snapshot by installPath[%s] on external primary storage[uuid:%s] to prepare encrypted snapshot image cache[uuid:%s]",
                    srcInstallPath, primaryStorageUuid, dst.getUuid()));
        }

        String hostUuid = findConnectedKvmHost(primaryStorageUuid, String.format(
                "prepare LUKS secret for snapshot image cache[uuid:%s]", dst.getUuid()));
        String encryptedDek = snapshotEncryptionHelper.prepareTemporarySnapshotImageEncryptedDek(
                hostUuid, snapshotUuid, dst.getUuid(), true);
        if (StringUtils.isBlank(encryptedDek)) {
            throw new OperationFailureException(operr(
                    "cannot prepare LUKS encryptedDek for snapshot image cache[uuid:%s] from snapshot[uuid:%s] on host[uuid:%s]",
                    dst.getUuid(), snapshotUuid, hostUuid));
        }
        return new ZbsVolumeEncryptionMaterial(hostUuid, encryptedDek);
    }

    private String findConnectedKvmHost(String primaryStorageUuid, String operation) {
        ErrorableValue<String> host = volumeManager.findConnectedKvmHostByPrimaryStorage(
                primaryStorageUuid, "external primary storage", operation);
        if (!host.isSuccess()) {
            throw new OperationFailureException(host.error);
        }
        return host.result;
    }
}
