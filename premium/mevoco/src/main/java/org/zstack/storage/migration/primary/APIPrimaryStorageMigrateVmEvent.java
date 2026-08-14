package org.zstack.storage.migration.primary;

import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeState;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;
import java.util.Arrays;

@RestResponse(allTo = "inventory")
public class APIPrimaryStorageMigrateVmEvent extends APIEvent {
    private VmInstanceInventory inventory;

    public APIPrimaryStorageMigrateVmEvent() {
    }

    public APIPrimaryStorageMigrateVmEvent(String apiId) {
        super(apiId);
    }

    public static APIPrimaryStorageMigrateVmEvent __example__() {
        APIPrimaryStorageMigrateVmEvent evt = new APIPrimaryStorageMigrateVmEvent();


        String defaultL3Uuid = uuid();
        String rootVolumeUuid = uuid();
        String dataVolumeUuid = uuid();

        VmInstanceInventory vm = new VmInstanceInventory();
        vm.setName("Test-VM");
        vm.setUuid(uuid());
        vm.setAllocatorStrategy(HostAllocatorConstant.LAST_HOST_PREFERRED_ALLOCATOR_STRATEGY_TYPE);
        vm.setClusterUuid(uuid());
        vm.setCpuNum(1);
        vm.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vm.setDefaultL3NetworkUuid(defaultL3Uuid);
        vm.setDescription("web server VM");
        vm.setHostUuid(uuid());
        vm.setHypervisorType("KVM");
        vm.setImageUuid(uuid());
        vm.setInstanceOfferingUuid(uuid());
        vm.setLastHostUuid(uuid());
        vm.setMemorySize(SizeUnit.GIGABYTE.toByte(8));
        vm.setPlatform("Linux");
        vm.setRootVolumeUuid(rootVolumeUuid);
        vm.setState(VmInstanceState.Running.toString());
        vm.setType(VmInstanceConstant.USER_VM_TYPE);
        vm.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vm.setZoneUuid(uuid());

        VolumeInventory vol = new VolumeInventory();
        vol.setName(String.format("Root-Volume-For-VM-%s", vm.getUuid()));
        vol.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vol.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vol.setType(VolumeType.Root.toString());
        vol.setUuid(rootVolumeUuid);
        vol.setSize(SizeUnit.GIGABYTE.toByte(100));
        vol.setActualSize(SizeUnit.GIGABYTE.toByte(20));
        vol.setDeviceId(0);
        vol.setState(VolumeState.Enabled.toString());
        vol.setFormat("qcow2");
        vol.setDiskOfferingUuid(uuid());
        vol.setInstallPath(String.format("/zstack_ps/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-%s/%s.qcow2", rootVolumeUuid, rootVolumeUuid));
        vol.setStatus(VolumeStatus.Ready.toString());
        vol.setPrimaryStorageUuid(uuid());
        vol.setVmInstanceUuid(vm.getUuid());
        vol.setRootImageUuid(vm.getImageUuid());

        VolumeInventory vol1 = new VolumeInventory();
        vol1.setName(String.format("Data-Volume-For-VM-%s", vm.getUuid()));
        vol1.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vol1.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vol1.setType(VolumeType.Data.toString());
        vol1.setUuid(dataVolumeUuid);
        vol1.setSize(SizeUnit.GIGABYTE.toByte(500));
        vol1.setActualSize(SizeUnit.GIGABYTE.toByte(100));
        vol1.setDeviceId(1);
        vol1.setState(VolumeState.Enabled.toString());
        vol1.setFormat("qcow2");
        vol1.setDiskOfferingUuid(uuid());
        vol1.setInstallPath(String.format("/zstack_ps/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-%s/%s.qcow2", dataVolumeUuid, dataVolumeUuid));
        vol1.setStatus(VolumeStatus.Ready.toString());
        vol1.setPrimaryStorageUuid(uuid());
        vol1.setVmInstanceUuid(vm.getUuid());

        vm.setAllVolumes(Arrays.asList(vol, vol1));

        VmNicInventory nic = new VmNicInventory();
        nic.setVmInstanceUuid(vm.getUuid());
        nic.setCreateDate(vm.getCreateDate());
        nic.setLastOpDate(vm.getLastOpDate());
        nic.setDeviceId(0);
        nic.setGateway("192.168.1.1");
        nic.setIp("192.168.1.10");
        nic.setL3NetworkUuid(defaultL3Uuid);
        nic.setNetmask("255.255.255.0");
        nic.setMac("00:0c:29:bd:99:fc");
        nic.setHypervisorType("KVM");
        nic.setUsedIpUuid(uuid());
        nic.setUuid(uuid());
        vm.setVmNics(Arrays.asList(nic));

        evt.setInventory(vm);
        return evt;
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
