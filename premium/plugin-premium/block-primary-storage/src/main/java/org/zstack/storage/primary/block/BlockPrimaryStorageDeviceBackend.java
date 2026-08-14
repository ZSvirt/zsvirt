package org.zstack.storage.primary.block;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.TakeSnapshotOnHypervisorReply;
import org.zstack.header.storage.primary.DeleteSnapshotOnPrimaryStorageMsg;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeProvisioningStrategy;
import org.zstack.sdk.IscsiServerInventory;
import org.zstack.storage.primary.PrimaryStorageBase;

import java.util.List;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/3/29 18:03
 */
public interface BlockPrimaryStorageDeviceBackend {

    void createLun(BlockScsiLunVO blockScsiLunVO, VolumeProvisioningStrategy volumeProvisioningStrategy, ReturnValueCompletion<BlockScsiLunVO> completion);

    void getLunByName(String name, ReturnValueCompletion<BlockScsiLunVO> completion);

    void resizeLun(BlockScsiLunVO blockScsiLunVO, long resize, Completion completion);

    void updateLunName(BlockScsiLunVO blockScsiLunVO, String newName, Completion completion);

    void deleteLun(Integer lunId, Completion completion);

    void createLunMap(BlockScsiLunVO blockScsiLunVO, BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO, ReturnValueCompletion<BlockScsiLunVO> completion);

    void deleteLunMap(BlockScsiLunVO blockScsiLunVO, BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO, Completion completion);

    void checkLunHasMappedToHost(BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO, BlockScsiLunVO blockScsiLunVO, ReturnValueCompletion<Boolean> completion);

    void addHost(BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO, ReturnValueCompletion<BlockPrimaryStorageHostRefVO> completion);

    void deleteHost(String metadata, Completion completion);

    void createHeartbeat(List<BlockPrimaryStorageHostRefVO> blockPrimaryStorageHostRefVOS, String primaryStorageUuid, BlockScsiLunVO blockScsiLunVO, ReturnValueCompletion<BlockScsiLunVO> completion);

    String getVendorName();

    <T> T getBlockPrimaryStorageDeviceBackend(BlockPrimaryStorageVO bpsvo);

    void setMetadata(String metadata);

    String syncMetadata();

    String getMetadata();

    IscsiServerInventory getIscsiServer(BlockScsiLunVO blockScsiLunVO);

    void getPhysicalCapacityUsage(ReturnValueCompletion<PrimaryStorageBase.PhysicalCapacityUsage> completion);

    void createLunFromTemplate(BlockScsiLunVO blockScsiLunVO, String name, VolumeProvisioningStrategy volumeProvisioningStrategy, ReturnValueCompletion<BlockScsiLunVO> completion);

    void createLunFromSnapshot(BlockScsiLunVO blockScsiLunVO, String name, VolumeProvisioningStrategy volumeProvisioningStrategy, ReturnValueCompletion<BlockScsiLunVO> completion);

    void takeSnapshot(BlockScsiLunVO blockScsiLunVO, ReturnValueCompletion<TakeSnapshotOnHypervisorReply> completion);

    void deleteSnapshot(DeleteSnapshotOnPrimaryStorageMsg msg, Completion completion);

    void revertSnapshot(BlockScsiLunVO blockScsiLunVO, VolumeSnapshotInventory snapshotInventory, ReturnValueCompletion<BlockScsiLunVO> completion);

    void checkBits(String lunName, Long actualSize, ReturnValueCompletion<Boolean> completion);

    void pingHook(Completion completion);

    void checkLunSession(BlockScsiLunVO blockScsiLunVO, String hostUuid, Completion completion);

    void getLunMappedHosts(BlockScsiLunVO blockScsiLunVO, ReturnValueCompletion<List<String>> completion);

    void getImageCacheLun(String name, ReturnValueCompletion<BlockScsiLunVO> completion);
}
