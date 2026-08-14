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

import static java.util.Arrays.asList;

/**
 * @apiResult api event for message :ref:`APIAttachNicToVmMsg`
 * @example {
 * "org.zstack.header.vm.APIAttachNicToVmEvent": {
 * "inventory": {
 * "uuid": "94d991c631674b16be65bfdf28b9e84a",
 * "name": "TestVm",
 * "description": "Test",
 * "zoneUuid": "acadddc85a604db4b1b7358605cd6015",
 * "clusterUuid": "f6cd5db05a0d49d8b12721e0bf721b4c",
 * "imageUuid": "061141410a0449b6919b50e90d68b7cd",
 * "hostUuid": "908131845d284d7f821a74362fff3d19",
 * "lastHostUuid": "908131845d284d7f821a74362fff3d19",
 * "instanceOfferingUuid": "91cb47f1416748afa7e0d34f4d0731ef",
 * "rootVolumeUuid": "19aa7ec504a247d89b511b322ffa483c",
 * "type": "UserVm",
 * "hypervisorType": "KVM",
 * "createDate": "Apr 30, 2014 6:11:47 PM",
 * "lastOpDate": "Apr 30, 2014 6:11:47 PM",
 * "state": "Running",
 * "internalId": 1,
 * "vmNics": [
 * {
 * "uuid": "6b58e6b2ba174ef4bce8a549de9560e8",
 * "vmInstanceUuid": "94d991c631674b16be65bfdf28b9e84a",
 * "usedIpUuid": "4ecc80a2d1d93d48b32680827542ddbb",
 * "l3NetworkUuid": "55f85b8fa9a647f1be251787c66550ee",
 * "ip": "10.12.140.148",
 * "mac": "fa:f0:08:8c:20:00",
 * "netmask": "255.0.0.0",
 * "gateway": "10.10.2.1",
 * "internalName": "vnic1.0",
 * "deviceId": 0,
 * "createDate": "Apr 30, 2014 6:11:47 PM",
 * "lastOpDate": "Apr 30, 2014 6:11:47 PM"
 * },
 * {
 * "uuid": "889cfcab8c08409296c649611a4df50c",
 * "vmInstanceUuid": "94d991c631674b16be65bfdf28b9e84a",
 * "usedIpUuid": "8877537e11783ee0bfe8af0fcf7a6388",
 * "l3NetworkUuid": "c6134efd3af94db7b2928ddc5deba540",
 * "ip": "10.4.224.72",
 * "mac": "fa:e3:87:b1:71:01",
 * "netmask": "255.0.0.0",
 * "gateway": "10.0.0.1",
 * "internalName": "vnic1.1",
 * "deviceId": 1,
 * "createDate": "Apr 30, 2014 6:11:47 PM",
 * "lastOpDate": "Apr 30, 2014 6:11:47 PM"
 * },
 * {
 * "uuid": "cba0da7a12d44b2e878dd5803d078337",
 * "vmInstanceUuid": "94d991c631674b16be65bfdf28b9e84a",
 * "usedIpUuid": "f90d01a098303956823ced02438ae3ab",
 * "l3NetworkUuid": "c7e9e14f2af742c29c3e25d58f16a45f",
 * "ip": "10.29.42.155",
 * "mac": "fa:2d:31:08:da:02",
 * "netmask": "255.0.0.0",
 * "gateway": "10.20.3.1",
 * "internalName": "vnic1.2",
 * "deviceId": 2,
 * "createDate": "Apr 30, 2014 6:11:47 PM",
 * "lastOpDate": "Apr 30, 2014 6:11:47 PM"
 * },
 * {
 * "uuid": "f31e38309e2047beac588e111fa2051f",
 * "vmInstanceUuid": "94d991c631674b16be65bfdf28b9e84a",
 * "usedIpUuid": "4ce077085c7e355d988450f11ce767b7",
 * "l3NetworkUuid": "e438b93332ba40dcbb5d553c749a43ca",
 * "ip": "10.20.206.157",
 * "mac": "fa:a3:04:b2:6c:00",
 * "netmask": "255.0.0.0",
 * "gateway": "10.20.4.1",
 * "internalName": "vnic1.0",
 * "deviceId": 0,
 * "createDate": "Apr 30, 2014 6:11:48 PM",
 * "lastOpDate": "Apr 30, 2014 6:11:48 PM"
 * }
 * ],
 * "allVolumes": [
 * {
 * "uuid": "19aa7ec504a247d89b511b322ffa483c",
 * "name": "ROOT-for-TestVm",
 * "description": "Root volume for VM[uuid:94d991c631674b16be65bfdf28b9e84a]",
 * "primaryStorageUuid": "24931b95b45e41fb8e41a640302d4c00",
 * "vmInstanceUuid": "94d991c631674b16be65bfdf28b9e84a",
 * "rootImageUuid": "061141410a0449b6919b50e90d68b7cd",
 * "installPath": "/opt/zstack/nfsprimarystorage/prim-24931b95b45e41fb8e41a640302d4c00/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-19aa7ec504a247d89b511b322ffa483c/19aa7ec504a247d89b511b322ffa483c.qcow2",
 * "type": "Root",
 * "hypervisorType": "KVM",
 * "size": 32212254720,
 * "deviceId": 0,
 * "state": "Enabled",
 * "status": "Ready",
 * "createDate": "Apr 30, 2014 6:11:47 PM",
 * "lastOpDate": "Apr 30, 2014 6:11:47 PM",
 * "backupStorageRefs": []
 * }
 * ]
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */

@RestResponse(allTo = "inventory")
public class APIAttachL3NetworkToVmEvent extends APIEvent {
    /**
     * @desc see :ref:`VmInstanceInventory`
     */
    private VmInstanceInventory inventory;

    public APIAttachL3NetworkToVmEvent() {
    }

    public APIAttachL3NetworkToVmEvent(String apiId) {
        super(apiId);
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAttachL3NetworkToVmEvent __example__() {
        APIAttachL3NetworkToVmEvent event = new APIAttachL3NetworkToVmEvent();

        String defaultL3Uuid = uuid();
        String rootVolumeUuid = uuid();

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
        vm.setAllVolumes(asList(vol));

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
        vm.setVmNics(asList(nic));

        event.setInventory(vm);


        return event;
    }

}
