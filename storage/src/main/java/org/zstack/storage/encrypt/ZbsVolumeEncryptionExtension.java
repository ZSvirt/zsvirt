package org.zstack.storage.encrypt;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.addon.primary.CreateVolumeSpec;
import org.zstack.header.storage.addon.primary.ZbsVolumeEncryptionBackend;
import org.zstack.header.storage.addon.primary.ZbsVolumeEncryptionExtensionPoint;
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageReply;
import org.zstack.header.storage.primary.ConvertVolumeEncryptionOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.ConvertVolumeEncryptionOnPrimaryStorageReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStats;
import org.zstack.header.volume.VolumeVO;

public class ZbsVolumeEncryptionExtension implements ZbsVolumeEncryptionExtensionPoint {
    @Autowired
    private ZbsEncryptedEmptyVolumeCreator emptyVolumeCreator;
    @Autowired
    private ZbsEncryptedVolumeCloner volumeCloner;
    @Autowired
    private ZbsEncryptedSnapshotVolumeCopier snapshotVolumeCopier;
    @Autowired
    private ZbsVolumeEncryptInPlaceHandler encryptInPlaceHandler;
    @Autowired
    private ZbsVolumeEncryptionConverter volumeEncryptionConverter;
    @Autowired
    private ZbsEncryptedVolumeResizer volumeResizer;
    @Autowired
    private VolumeSnapshotEncryptionHelper snapshotEncryptionHelper;

    @Override
    public void createEncryptedEmptyVolume(ZbsVolumeEncryptionBackend backend, CreateVolumeSpec spec,
                                           ReturnValueCompletion<VolumeStats> completion) {
        emptyVolumeCreator.create(backend, spec, completion);
    }

    @Override
    public void cloneEncryptedVolumeFromImage(ZbsVolumeEncryptionBackend backend, String srcInstallPath,
                                              CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats> completion) {
        volumeCloner.cloneFromImage(backend, srcInstallPath, dst, completion);
    }

    @Override
    public void copyEncryptedVolumeFromSnapshot(ZbsVolumeEncryptionBackend backend, String srcInstallPath,
                                                CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats> completion) {
        snapshotVolumeCopier.copy(backend, srcInstallPath, dst, completion);
    }

    @Override
    public void encryptVolumeBits(ZbsVolumeEncryptionBackend backend, EncryptVolumeBitsOnPrimaryStorageMsg msg,
                                  ReturnValueCompletion<EncryptVolumeBitsOnPrimaryStorageReply> completion) {
        encryptInPlaceHandler.encrypt(backend, msg, completion);
    }

    @Override
    public void convertVolumeEncryption(ZbsVolumeEncryptionBackend backend,
                                        ConvertVolumeEncryptionOnPrimaryStorageMsg msg,
                                        ReturnValueCompletion<ConvertVolumeEncryptionOnPrimaryStorageReply> completion) {
        volumeEncryptionConverter.convert(backend, msg, completion);
    }

    @Override
    public void resizeEncryptedVolume(String primaryStorageUuid, VolumeInventory volume, long size,
                                      ReturnValueCompletion<VolumeStats> completion) {
        volumeResizer.resize(primaryStorageUuid, volume, size, completion);
    }

    @Override
    public void beforeTakeSnapshot(String primaryStorageUuid, VolumeVO volume, VolumeSnapshotInventory snapshot) {
        if (volume == null || !volume.isEncrypted()) {
            return;
        }
        snapshotEncryptionHelper.inheritVolumeKeyToSnapshot(volume, snapshot);
    }

    @Override
    public void afterTakeSnapshot(String primaryStorageUuid, VolumeVO volume, VolumeSnapshotInventory snapshot) {
        if (volume == null || !volume.isEncrypted()) {
            return;
        }
        snapshotEncryptionHelper.completeTakeSnapshot(volume, snapshot);
    }
}
