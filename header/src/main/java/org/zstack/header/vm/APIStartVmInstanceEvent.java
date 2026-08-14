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
 * @apiResult api event for message :ref:`APIStartVmInstanceMsg`
 * @example {
 * "org.zstack.header.vm.APIStartVmInstanceEvent": {
 * "inventory": {
 * "uuid": "0187ba07f22e431a9cc0ee308a44ad57",
 * "name": "TestVm",
 * "description": "Test",
 * "zoneUuid": "7cdb2374dd0b4e548274937694bf7c80",
 * "clusterUuid": "0c221d978b8a4f82a94a72917020d138",
 * "imageUuid": "68d41d4ef443444094dd3c2330acd7a5",
 * "hostUuid": "4a640767e836463a901f6f0cc685c93b",
 * "instanceOfferingUuid": "73532ef4bad5490c85209d047d0b9b59",
 * "rootVolumeUuid": "789ba5ff9836442ba3877bf556e2ada2",
 * "type": "UserVm",
 * "hypervisorType": "Simulator",
 * "createDate": "Apr 30, 2014 9:24:54 PM",
 * "lastOpDate": "Apr 30, 2014 9:24:54 PM",
 * "state": "Running",
 * "internalId": 1,
 * "vmNics": [
 * {
 * "uuid": "b7b3b38ba1a94ecfa187879096bc9cf1",
 * "vmInstanceUuid": "0187ba07f22e431a9cc0ee308a44ad57",
 * "usedIpUuid": "2123c4c1dfca30c39d6c8ab45759b8c5",
 * "l3NetworkUuid": "107460191ba14afe8a48081d8f11385c",
 * "ip": "10.9.167.45",
 * "mac": "fa:d4:ab:80:59:00",
 * "netmask": "255.0.0.0",
 * "gateway": "10.0.0.1",
 * "internalName": "vnic1.0",
 * "deviceId": 0,
 * "createDate": "Apr 30, 2014 9:24:54 PM",
 * "lastOpDate": "Apr 30, 2014 9:24:54 PM"
 * },
 * {
 * "uuid": "323cc2ed15fb4d018892475af7deba73",
 * "vmInstanceUuid": "0187ba07f22e431a9cc0ee308a44ad57",
 * "usedIpUuid": "764d728cd14d3c2eaa3eb03b90dff2eb",
 * "l3NetworkUuid": "ea89fd29942f40e2855f2e70bed5b5ea",
 * "ip": "10.16.194.244",
 * "mac": "fa:38:05:42:a9:02",
 * "netmask": "255.0.0.0",
 * "gateway": "10.10.2.1",
 * "internalName": "vnic1.2",
 * "deviceId": 2,
 * "createDate": "Apr 30, 2014 9:24:54 PM",
 * "lastOpDate": "Apr 30, 2014 9:24:54 PM"
 * },
 * {
 * "uuid": "285afddec9e04a3187aae09bb0bbcec9",
 * "vmInstanceUuid": "0187ba07f22e431a9cc0ee308a44ad57",
 * "usedIpUuid": "43d254b24dae3b9ca0edd8661a964e3d",
 * "l3NetworkUuid": "1624223a9a584b72a23d02d021f3e3e7",
 * "ip": "10.22.103.16",
 * "mac": "fa:ce:41:1c:07:01",
 * "netmask": "255.0.0.0",
 * "gateway": "10.20.3.1",
 * "internalName": "vnic1.1",
 * "deviceId": 1,
 * "createDate": "Apr 30, 2014 9:24:54 PM",
 * "lastOpDate": "Apr 30, 2014 9:24:54 PM"
 * }
 * ],
 * "allVolumes": [
 * {
 * "uuid": "0e26d14fc5684c9f9d7c57de911f2359",
 * "name": "DATA",
 * "description": "DataVolume",
 * "primaryStorageUuid": "8d5686e6a38f4d458b54682a4e721bcc",
 * "vmInstanceUuid": "0187ba07f22e431a9cc0ee308a44ad57",
 * "installPath": "nfs:/test/vm/0e26d14fc5684c9f9d7c57de911f2359.qcow2",
 * "type": "Data",
 * "hypervisorType": "Simulator",
 * "size": 128849018880,
 * "deviceId": 1,
 * "state": "Enabled",
 * "status": "Ready",
 * "createDate": "Apr 30, 2014 9:24:54 PM",
 * "lastOpDate": "Apr 30, 2014 9:24:54 PM",
 * "backupStorageRefs": []
 * },
 * {
 * "uuid": "789ba5ff9836442ba3877bf556e2ada2",
 * "name": "ROOT-for-TestVm",
 * "description": "Root volume for VM[uuid:0187ba07f22e431a9cc0ee308a44ad57]",
 * "primaryStorageUuid": "8d5686e6a38f4d458b54682a4e721bcc",
 * "vmInstanceUuid": "0187ba07f22e431a9cc0ee308a44ad57",
 * "rootImageUuid": "68d41d4ef443444094dd3c2330acd7a5",
 * "installPath": "nfs:/test/vm/789ba5ff9836442ba3877bf556e2ada2.qcow2",
 * "type": "Root",
 * "hypervisorType": "Simulator",
 * "size": 0,
 * "deviceId": 0,
 * "state": "Enabled",
 * "status": "Ready",
 * "createDate": "Apr 30, 2014 9:24:54 PM",
 * "lastOpDate": "Apr 30, 2014 9:24:54 PM",
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
public class APIStartVmInstanceEvent extends APIEvent {
    /**
     * @desc see :ref:`VmInstanceInventory`
     */
    private VmInstanceInventory inventory;

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }

    public APIStartVmInstanceEvent(String apiId) {
        super(apiId);
    }

    public APIStartVmInstanceEvent() {
        super(null);
    }

    public static APIStartVmInstanceEvent __example__() {
        APIStartVmInstanceEvent evt = new APIStartVmInstanceEvent();

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

        evt.setInventory(vm);

        return evt;
    }

}
