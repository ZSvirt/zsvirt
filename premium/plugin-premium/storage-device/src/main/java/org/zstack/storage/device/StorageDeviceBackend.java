package org.zstack.storage.device;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostInventory;
import org.zstack.header.storageDevice.ScsiLunVO;
import org.zstack.header.storageDevice.ScsiLunVmInstanceRefVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.storage.device.fibreChannel.FiberChannelLunStruct;
import org.zstack.storage.device.hba.HbaDeviceStruct;
import org.zstack.storage.device.iscsi.IscsiServerInventory;
import org.zstack.storage.device.iscsi.IscsiServerVO;
import org.zstack.storage.device.localRaid.*;
import org.zstack.storage.device.multipath.DeviceTO;
import org.zstack.storage.device.nvme.NvmeLunStruct;
import org.zstack.storage.device.nvme.NvmeServerInventory;
import org.zstack.storage.device.nvme.NvmeServerVO;

import java.util.List;
import java.util.Map;

/**
 * Create by weiwang at 2018/8/3
 */
public interface StorageDeviceBackend {
    void loginIscsiServer(IscsiServerVO iscsiServerVO, HostInventory hostInventory, ReturnValueCompletion<IscsiServerInventory> completion);
    void logoutIscsiServer(IscsiServerVO iscsiServerVO, HostInventory hostInventory, Completion completion);

    void scanFcDevice(HostInventory hostInventory, boolean rescan, List<String> identifiers, ReturnValueCompletion<List<FiberChannelLunStruct>> completion);
    void scanNvmeDevice(HostInventory hostInventory, boolean rescan, List<String> identifiers, ReturnValueCompletion<List<NvmeLunStruct>> completion);
    void scanHba(HostInventory hostInventory, ReturnValueCompletion<List<HbaDeviceStruct>> completion);
    void connectNvmeServer(NvmeServerVO nvmeServerVO, HostInventory hostInventory, ReturnValueCompletion<NvmeServerInventory> completion);
    void disconnectNvmeServer(NvmeServerVO nvmeServerVO, HostInventory hostInventory, Completion completion);
    void enableMultipathDevice(HostInventory hostInventory, Completion completion);
    void disableMultipathDevice(HostInventory hostInventory, Completion completion);

    void attachScsiLunToVm(ScsiLunVO lunVO, VmInstanceVO vmInstanceVO, ScsiLunVmInstanceRefVO refvo, Completion completion);
    void detachScsiLunFromVm(ScsiLunVO lunVO, VmInstanceVO vmInstanceVO, ScsiLunVmInstanceRefVO refvo, Completion completion);
    void detachScsiLunFromHost(ScsiLunVO lunVO, String hostUuid, Completion completion);

    void scanLocalRaid(HostInventory hostInventory, ReturnValueCompletion<List<RaidPhysicalDriveStruct>> completion);
    void locateLocalRaidPhysicalDrive(RaidPhysicalDriveInventory raidPhysicalDrive, RaidControllerInventory controller, boolean locate, Completion completion);
    void getPhysicalDriveSmartData(RaidPhysicalDriveInventory raidPhysicalDrive, RaidControllerInventory controller, ReturnValueCompletion<List<SmartDataStruct>> completion);
    void localRaidSelfTest(RaidPhysicalDriveInventory raidPhysicalDrive, RaidControllerInventory controller, ReturnValueCompletion<SelfTestLocalRaidReply> completion);

    void getHostMultipathTopology(HostInventory hostInventory, List<String> wwids, ReturnValueCompletion<Map<String, List<DeviceTO>>> completion);
    String getSupportHypervisorType();
}
