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
 * @apiResult api event for message :ref:`APICreateVmInstanceFromVolumeMsg`
 * @example {
 * "org.zstack.header.vm.APICreateVmInstanceFromVolumeEvent": {
 * "inventory": {
 * "uuid": "e979b10eb753412e8588d26b4b544fdc",
 * "name": "TestVm",
 * "description": "Test",
 * "zoneUuid": "ab36c47da7e449e49966f66dcd6f9258",
 * "clusterUuid": "f48924e5173d49eba68d4163929edcbd",
 * "imageUuid": "99a5eea648954ef7be2b8ede8f34fe26",
 * "hostUuid": "572e908e23b749eeb6cf30b22d954bd0",
 * "lastHostUuid": "572e908e23b749eeb6cf30b22d954bd0",
 * "instanceOfferingUuid": "1618154b462a48749ca9b114cf4a2979",
 * "rootVolumeUuid": "252a500bc8744a61b11d90a69e9cb2a1",
 * "type": "UserVm",
 * "hypervisorType": "KVM",
 * "createDate": "Apr 30, 2014 7:50:18 PM",
 * "lastOpDate": "Apr 30, 2014 7:50:18 PM",
 * "state": "Running",
 * "internalId": 1,
 * "vmNics": [
 * {
 * "uuid": "e6524f1957b344ccbfd4aa2ff5c27d25",
 * "vmInstanceUuid": "e979b10eb753412e8588d26b4b544fdc",
 * "usedIpUuid": "748121497b483f28bd74692d5a5222da",
 * "l3NetworkUuid": "f5fbd96e0df745bdb7bc4f4c19febe65",
 * "ip": "10.19.252.235",
 * "mac": "fa:95:76:2a:9d:02",
 * "netmask": "255.0.0.0",
 * "gateway": "10.10.2.1",
 * "internalName": "vnic1.2",
 * "deviceId": 2,
 * "createDate": "Apr 30, 2014 7:50:18 PM",
 * "lastOpDate": "Apr 30, 2014 7:50:18 PM"
 * },
 * {
 * "uuid": "41a32a5ff14c4a588838628a3c373ed3",
 * "vmInstanceUuid": "e979b10eb753412e8588d26b4b544fdc",
 * "usedIpUuid": "0a7fbf19908c345b9d2ab9dd89ba128b",
 * "l3NetworkUuid": "c60285dca24d43a4b9a2e536674ddca1",
 * "ip": "10.28.7.140",
 * "mac": "fa:56:cb:20:08:01",
 * "netmask": "255.0.0.0",
 * "gateway": "10.20.3.1",
 * "internalName": "vnic1.1",
 * "deviceId": 1,
 * "createDate": "Apr 30, 2014 7:50:18 PM",
 * "lastOpDate": "Apr 30, 2014 7:50:18 PM"
 * },
 * {
 * "uuid": "6af9cde82647400eb674f3ede38c3caa",
 * "vmInstanceUuid": "e979b10eb753412e8588d26b4b544fdc",
 * "usedIpUuid": "959b4157ab43386994ea321f9a50fa71",
 * "l3NetworkUuid": "c4f6a370f80443798cc460ee07d56ff1",
 * "ip": "10.2.20.22",
 * "mac": "fa:d1:4d:77:d4:00",
 * "netmask": "255.0.0.0",
 * "gateway": "10.0.0.1",
 * "internalName": "vnic1.0",
 * "deviceId": 0,
 * "createDate": "Apr 30, 2014 7:50:18 PM",
 * "lastOpDate": "Apr 30, 2014 7:50:18 PM"
 * }
 * ],
 * "allVolumes": [
 * {
 * "uuid": "252a500bc8744a61b11d90a69e9cb2a1",
 * "name": "ROOT-for-TestVm",
 * "description": "Root volume for VM[uuid:e979b10eb753412e8588d26b4b544fdc]",
 * "primaryStorageUuid": "f79516b8ca5746fdbf271d56c0e6da3e",
 * "vmInstanceUuid": "e979b10eb753412e8588d26b4b544fdc",
 * "rootImageUuid": "99a5eea648954ef7be2b8ede8f34fe26",
 * "installPath": "/opt/zstack/nfsprimarystorage/prim-f79516b8ca5746fdbf271d56c0e6da3e/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-252a500bc8744a61b11d90a69e9cb2a1/252a500bc8744a61b11d90a69e9cb2a1.qcow2",
 * "type": "Root",
 * "hypervisorType": "KVM",
 * "size": 32212254720,
 * "deviceId": 0,
 * "state": "Enabled",
 * "status": "Ready",
 * "createDate": "Apr 30, 2014 7:50:18 PM",
 * "lastOpDate": "Apr 30, 2014 7:50:18 PM",
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
public class APICreateVmInstanceFromVolumeEvent extends APIEvent {
    /**
     * @desc see :ref:`VmInstanceInventory`
     */
    private VmInstanceInventory inventory;

    public APICreateVmInstanceFromVolumeEvent() {
        super(null);
    }

    public APICreateVmInstanceFromVolumeEvent(String apiId) {
        super(apiId);
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateVmInstanceFromVolumeEvent __example__() {
        APICreateVmInstanceFromVolumeEvent evt = new APICreateVmInstanceFromVolumeEvent();

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
