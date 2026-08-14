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
 * @apiResult api event for message :ref:`APIRebootVmInstanceMsg`
 * @example {
 * "org.zstack.header.vm.APIRebootVmInstanceEvent": {
 * "inventory": {
 * "uuid": "092a6a62ded0485590751e0c335e3005",
 * "name": "TestVm",
 * "description": "Test",
 * "zoneUuid": "139c4d23612e40ca8a9ab49af3e3d697",
 * "clusterUuid": "0fd3029a4f6d4f38bb0625da5bfd81e5",
 * "imageUuid": "db0f3ab83b9243e4bbfb3952f89861ad",
 * "hostUuid": "0409c17cd317468ba99f85fb3ceb46f8",
 * "lastHostUuid": "0409c17cd317468ba99f85fb3ceb46f8",
 * "instanceOfferingUuid": "4d606d8aa33349cba02ee42105b1ae49",
 * "rootVolumeUuid": "8e05d4f56fe04734bd06dbb90e06f2bd",
 * "type": "UserVm",
 * "hypervisorType": "KVM",
 * "createDate": "Apr 30, 2014 9:12:19 PM",
 * "lastOpDate": "Apr 30, 2014 9:12:19 PM",
 * "state": "Running",
 * "internalId": 1,
 * "vmNics": [
 * {
 * "uuid": "ea405e444f2c4d0480723c1570ca4df4",
 * "vmInstanceUuid": "092a6a62ded0485590751e0c335e3005",
 * "usedIpUuid": "d2c50952a7c63edca4dbb4de0e83634e",
 * "l3NetworkUuid": "05b969c7f55d4d09b3bf0d198a32be16",
 * "ip": "10.20.119.190",
 * "mac": "fa:1c:c5:5d:51:00",
 * "netmask": "255.0.0.0",
 * "gateway": "10.20.3.1",
 * "internalName": "vnic1.0",
 * "deviceId": 0,
 * "createDate": "Apr 30, 2014 9:12:19 PM",
 * "lastOpDate": "Apr 30, 2014 9:12:19 PM"
 * },
 * {
 * "uuid": "95a4a32a33a64d96b58c4d4bb473fe67",
 * "vmInstanceUuid": "092a6a62ded0485590751e0c335e3005",
 * "usedIpUuid": "1ac16628b6883edd854910fcd7136f91",
 * "l3NetworkUuid": "0862af13928e4f69a016c503cd2a670b",
 * "ip": "10.2.169.201",
 * "mac": "fa:4a:68:06:a6:01",
 * "netmask": "255.0.0.0",
 * "gateway": "10.0.0.1",
 * "internalName": "vnic1.1",
 * "deviceId": 1,
 * "createDate": "Apr 30, 2014 9:12:19 PM",
 * "lastOpDate": "Apr 30, 2014 9:12:19 PM"
 * },
 * {
 * "uuid": "a7936b97403b419f998787abf3d96456",
 * "vmInstanceUuid": "092a6a62ded0485590751e0c335e3005",
 * "usedIpUuid": "871179b74eb73837bff90399cab1e11a",
 * "l3NetworkUuid": "35d07cb5fbff4e32bf4d1b35e6518351",
 * "ip": "10.12.160.30",
 * "mac": "fa:80:3d:17:dd:02",
 * "netmask": "255.0.0.0",
 * "gateway": "10.10.2.1",
 * "internalName": "vnic1.2",
 * "deviceId": 2,
 * "createDate": "Apr 30, 2014 9:12:19 PM",
 * "lastOpDate": "Apr 30, 2014 9:12:19 PM"
 * }
 * ],
 * "allVolumes": [
 * {
 * "uuid": "8e05d4f56fe04734bd06dbb90e06f2bd",
 * "name": "ROOT-for-TestVm",
 * "description": "Root volume for VM[uuid:092a6a62ded0485590751e0c335e3005]",
 * "primaryStorageUuid": "53b2e3c563564afab8f3f918715be540",
 * "vmInstanceUuid": "092a6a62ded0485590751e0c335e3005",
 * "rootImageUuid": "db0f3ab83b9243e4bbfb3952f89861ad",
 * "installPath": "/opt/zstack/nfsprimarystorage/prim-53b2e3c563564afab8f3f918715be540/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-8e05d4f56fe04734bd06dbb90e06f2bd/8e05d4f56fe04734bd06dbb90e06f2bd.qcow2",
 * "type": "Root",
 * "hypervisorType": "KVM",
 * "size": 32212254720,
 * "deviceId": 0,
 * "state": "Enabled",
 * "status": "Ready",
 * "createDate": "Apr 30, 2014 9:12:19 PM",
 * "lastOpDate": "Apr 30, 2014 9:12:19 PM",
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
public class APIRebootVmInstanceEvent extends APIEvent {
    /**
     * @desc see :ref:`VmInstanceInventory`
     */
    private VmInstanceInventory inventory;

    public APIRebootVmInstanceEvent(String apiId) {
        super(apiId);
    }

    public APIRebootVmInstanceEvent() {
        super(null);
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIRebootVmInstanceEvent __example__() {
        APIRebootVmInstanceEvent event = new APIRebootVmInstanceEvent();

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
