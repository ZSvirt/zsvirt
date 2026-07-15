package org.zstack.header.storage.addon.primary;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageReply;
import org.zstack.header.storage.primary.ConvertVolumeEncryptionOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.ConvertVolumeEncryptionOnPrimaryStorageReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStats;
import org.zstack.header.volume.VolumeVO;

public interface ZbsVolumeEncryptionExtensionPoint {
    String PRIMARY_STORAGE_IDENTITY = "zbs";

    void createEncryptedEmptyVolume(ZbsVolumeEncryptionBackend backend, CreateVolumeSpec spec,
                                    ReturnValueCompletion<VolumeStats> completion);

    void cloneEncryptedVolumeFromImage(ZbsVolumeEncryptionBackend backend, String srcInstallPath, CreateVolumeSpec dst,
                                       ReturnValueCompletion<VolumeStats> completion);

    void copyEncryptedVolumeFromSnapshot(ZbsVolumeEncryptionBackend backend, String srcInstallPath, CreateVolumeSpec dst,
                                         ReturnValueCompletion<VolumeStats> completion);

    void encryptVolumeBits(ZbsVolumeEncryptionBackend backend, EncryptVolumeBitsOnPrimaryStorageMsg msg,
                           ReturnValueCompletion<EncryptVolumeBitsOnPrimaryStorageReply> completion);

    void convertVolumeEncryption(ZbsVolumeEncryptionBackend backend, ConvertVolumeEncryptionOnPrimaryStorageMsg msg,
                                 ReturnValueCompletion<ConvertVolumeEncryptionOnPrimaryStorageReply> completion);

    void resizeEncryptedVolume(String primaryStorageUuid, VolumeInventory volume, long size,
                               ReturnValueCompletion<VolumeStats> completion);

    void beforeTakeSnapshot(String primaryStorageUuid, VolumeVO volume, VolumeSnapshotInventory snapshot);

    void afterTakeSnapshot(String primaryStorageUuid, VolumeVO volume, VolumeSnapshotInventory snapshot);
}
