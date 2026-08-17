package org.zstack.header.vm;

import org.zstack.header.configuration.PythonClass;
import org.zstack.utils.data.SizeUnit;

@PythonClass
public interface VmInstanceConstant {
    String SERVICE_ID = "vmInstance";
    String SECURE_BOOT_SERVICE_ID = "secureBoot";
    String ACTION_CATEGORY = "instance";
    @PythonClass
    String USER_VM_TYPE = "UserVm";
    Integer VM_MONITOR_NUMBER = 1;

    // System limit
    int MAXIMUM_CDROM_NUMBER = 3;

    String KVM_HYPERVISOR_TYPE = "KVM";

    String VIRTUAL_NIC_TYPE = "VNIC";

    String VM_SYNC_SIGNATURE_PREFIX = "Vm-";

    String TF_VIRTUAL_NIC_TYPE = "TFVNIC";

    String SHUTDOWN_DETAIL_BY_HOST = "by host";
    String SHUTDOWN_DETAIL_BY_GUEST = "by guest";
    String SHUTDOWN_DETAIL_FINISHED = "finished";

    enum Params {
        VmInstanceSpec,
        AttachingVolumeInventory,
        AttachedDataVolumeInventories,
        DestPrimaryStorageInventoryForAttachingVolume,
        AttachNicInventory,
        AbnormalLifeCycleStruct,
        DeletionPolicy,
        AttachingIsoInventory,
        DetachingIsoUuid,
        ignoreDetachError,
        ReleaseNicAfterDetachNic,
        VmNicInventory,
        L3NetworkInventory,
        UsedIPInventory,
        vmInventory,
        vmInstanceUuid,
        VmAllocateNicFlow_ips,
        VmAllocateNicFlow_nics,
        VmAllocateNicFlow_allowDuplicatedAddress,
        VmAllocateNicFlow_nicNetworkInfo,
        ApplianceVmSyncHaConfig_applianceVm,
        ApplianceVmSyncHaConfig_haUuid,
        AllocatedUrlForAttachingVolume,
        VmAllocateNicFlow_allowDuplicatedMac,
        VmNicSecurityPolicyByDeviceId,

        VmInstanceUuid,
    }

    enum VmOperation {
        NewCreate,
        Start,
        Stop,
        Pause,
        Resume,
        Reboot,
        Destroy,
        Migrate,
        AttachVolume,
        AttachNic,
        ChangeNicNetwork,
        ChangeNicIp,
        DetachNic,
        ChangeNicState,
        AttachIso,
        DetachIso,
        Expunge,
        ChangeImage,
        ChangePassword,
        SetBootMode,
        Update,
        SetConsolePassword,
        SetVmQga
    }

    String USER_VM_REGEX_PASSWORD = "[\\da-zA-Z-`=\\\\\\[\\];',./~!@#$%^&*()_+|{}:\"<>?]{0,}";

    enum Capability {
        LiveMigration,
        VolumeMigration,
        Reimage,
        MemorySnapshot
    }

    String EMPTY_CDROM = "empty";
    String NONE_CDROM = "none";

    String DETACH_NIC_FAILED_REGEX = ".*NIC device is still attached after.*";

    String VM_CDROM_OCCUPANT_ISO = "ISO";
    String VM_CDROM_OCCUPANT_GUEST_TOOLS = "GuestTools";
}
