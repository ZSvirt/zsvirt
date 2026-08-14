package org.zstack.header.vm;

import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeState;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(fieldsTo = {"all"})
public class APICreateVmInstanceFromTemplatedVmInstanceEvent extends APIEvent {
    private CreateVmInstanceFromTemplatedVmInstanceResults result;

    public APICreateVmInstanceFromTemplatedVmInstanceEvent() {
        super(null);
    }

    public APICreateVmInstanceFromTemplatedVmInstanceEvent(String apiId) {
        super(apiId);
    }

    public CreateVmInstanceFromTemplatedVmInstanceResults getResult() {
        return result;
    }

    public void setResult(CreateVmInstanceFromTemplatedVmInstanceResults result) {
        this.result = result;
    }

    public static APICreateVmInstanceFromTemplatedVmInstanceEvent __example__() {
        APICreateVmInstanceFromTemplatedVmInstanceEvent event = new APICreateVmInstanceFromTemplatedVmInstanceEvent();
        CreateVmInstanceFromTemplatedVmInstanceResults result = new CreateVmInstanceFromTemplatedVmInstanceResults();

        String defaultL3Uuid = uuid();
        String rootVolumeUuid = uuid();

        VmInstanceInventory vm = new VmInstanceInventory();
        vm.setName("new vm name");
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
        vm.setAllVolumes(asList(vol));


        List<CloneVmInstanceInventory> inventories = new ArrayList<>();
        CloneVmInstanceInventory ccInventorie = new CloneVmInstanceInventory();
        ccInventorie.setInventory(vm);

        inventories.add(ccInventorie);
        result.setInventories(inventories);
        result.setNumberOfClonedVm(1);
        event.setResult(result);
        return event;
    }
}
