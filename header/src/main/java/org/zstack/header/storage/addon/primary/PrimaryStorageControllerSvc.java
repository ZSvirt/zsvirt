package org.zstack.header.storage.addon.primary;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostInventory;
import org.zstack.header.storage.addon.*;
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageReply;
import org.zstack.header.storage.primary.ConvertVolumeEncryptionOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.ConvertVolumeEncryptionOnPrimaryStorageReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotStats;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.header.volume.VolumeStats;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

public interface PrimaryStorageControllerSvc {
    String getIdentity();
    void connect(String config, String url, ReturnValueCompletion<LinkedHashMap> comp);
    void ping(Completion completion);

    void reportCapacity(ReturnValueCompletion<StorageCapacity> comp);
    void reportHealthy(ReturnValueCompletion<StorageHealthy> comp);
    void reportNodeHealthy(HostInventory host, ReturnValueCompletion<NodeHealthy> comp);
    StorageCapabilities reportCapabilities();

    String allocateSpace(AllocateSpaceSpec aspec);

    void createVolume(CreateVolumeSpec v, ReturnValueCompletion<VolumeStats>comp);
    void deleteVolume(String installPath, Completion comp);
    void deleteVolumeAndSnapshot(String installPath, Completion comp);
    void trashVolume(String installPath, Completion comp);
    void cloneVolume(String srcInstallPath, CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats>comp);
    void copyVolume(String srcInstallPath, CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats>comp);
    void encryptVolumeBits(EncryptVolumeBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<EncryptVolumeBitsOnPrimaryStorageReply> comp);
    void convertVolumeEncryption(ConvertVolumeEncryptionOnPrimaryStorageMsg msg,
                                 ReturnValueCompletion<ConvertVolumeEncryptionOnPrimaryStorageReply> completion);
    void flattenVolume(String installPath, ReturnValueCompletion<VolumeStats>comp);

    // support uri or path
    void stats(String installPath, ReturnValueCompletion<VolumeStats> comp);

    void batchStats(Collection<String> installPath, ReturnValueCompletion<List<VolumeStats>> comp);

    void expandVolume(VolumeInventory volume, long size, ReturnValueCompletion<VolumeStats> comp);
    void setVolumeQos(BaseVolumeInfo v, Completion comp);
    void deleteVolumeQos(BaseVolumeInfo v, Completion comp);
    void export(ExportSpec espec, VolumeProtocol protocol, ReturnValueCompletion<RemoteTarget> comp);
    void unexport(ExportSpec espec, RemoteTarget remoteTarget, VolumeProtocol protocol, Completion comp);

    void createSnapshot(CreateVolumeSnapshotSpec spec, ReturnValueCompletion<VolumeSnapshotStats> comp);
    void deleteSnapshot(String installPath, Completion comp);
    void expungeSnapshot(String installPath, Completion comp);
    void revertVolumeSnapshot(String snapshotInstallPath, ReturnValueCompletion<VolumeStats> comp);

    void validateConfig(String config);

    void setTrashExpireTime(int timeInSeconds, Completion completion);
    void onFirstAdditionConfigure(Completion completion);

    long alignSize(long size);
}
