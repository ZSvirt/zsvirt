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
 * @apiResult api event for message :ref:`APIMigrateVmMsg`
 * @example {
 * "org.zstack.header.vm.APIMigrateVmEvent": {
 * "inventory": {
 * "uuid": "7fed8055cc004d8fbafd2918efdb2fcc",
 * "name": "TestVm",
 * "description": "Test",
 * "zoneUuid": "dc7d9cd3b1ab428482769f926d9c3c6f",
 * "clusterUuid": "0b42fc0e604e4f35a0330c5bace3e746",
 * "imageUuid": "b375e0dc19384b96ae9bb082b9fcbcf1",
 * "hostUuid": "4796eba438884c49b6d754b33f84d582",
 * "lastHostUuid": "a4a789ef35eb48f5a30bce548d733bbf",
 * "instanceOfferingUuid": "9a557deea6b347c6900b3ea81f7f0a4a",
 * "rootVolumeUuid": "755aec30fa35469f9223e72204ad1ee0",
 * "type": "UserVm",
 * "hypervisorType": "KVM",
 * "createDate": "Apr 30, 2014 9:03:19 PM",
 * "lastOpDate": "Apr 30, 2014 9:03:19 PM",
 * "state": "Running",
 * "internalId": 1,
 * "vmNics": [
 * {
 * "uuid": "a0afca2a84d44dcd8fb322c24158e323",
 * "vmInstanceUuid": "7fed8055cc004d8fbafd2918efdb2fcc",
 * "usedIpUuid": "1bad097f9f6a3d87b71eb7febd90beb2",
 * "l3NetworkUuid": "6313310e72d6444dba22537e0d69ef02",
 * "ip": "10.4.65.8",
 * "mac": "fa:52:f2:e2:47:01",
 * "netmask": "255.0.0.0",
 * "gateway": "10.0.0.1",
 * "internalName": "vnic1.1",
 * "deviceId": 1,
 * "createDate": "Apr 30, 2014 9:03:19 PM",
 * "lastOpDate": "Apr 30, 2014 9:03:19 PM"
 * },
 * {
 * "uuid": "e62ae39ae9e04ce3a1e4771e4ec3633b",
 * "vmInstanceUuid": "7fed8055cc004d8fbafd2918efdb2fcc",
 * "usedIpUuid": "3f599ce929c2332c948f64109cd5f17c",
 * "l3NetworkUuid": "5eff051c6c9d4dbf86894129c2f68f4e",
 * "ip": "10.16.114.205",
 * "mac": "fa:46:ed:72:00:00",
 * "netmask": "255.0.0.0",
 * "gateway": "10.10.2.1",
 * "internalName": "vnic1.0",
 * "deviceId": 0,
 * "createDate": "Apr 30, 2014 9:03:19 PM",
 * "lastOpDate": "Apr 30, 2014 9:03:19 PM"
 * }
 * ],
 * "allVolumes": [
 * {
 * "uuid": "755aec30fa35469f9223e72204ad1ee0",
 * "name": "ROOT-for-TestVm",
 * "description": "Root volume for VM[uuid:7fed8055cc004d8fbafd2918efdb2fcc]",
 * "primaryStorageUuid": "c0e9cfee937e41b5ad4d3ba2c710b7d2",
 * "vmInstanceUuid": "7fed8055cc004d8fbafd2918efdb2fcc",
 * "rootImageUuid": "b375e0dc19384b96ae9bb082b9fcbcf1",
 * "installPath": "/opt/zstack/nfsprimarystorage/prim-c0e9cfee937e41b5ad4d3ba2c710b7d2/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-755aec30fa35469f9223e72204ad1ee0/755aec30fa35469f9223e72204ad1ee0.qcow2",
 * "type": "Root",
 * "hypervisorType": "KVM",
 * "size": 32212254720,
 * "deviceId": 0,
 * "state": "Enabled",
 * "status": "Ready",
 * "createDate": "Apr 30, 2014 9:03:19 PM",
 * "lastOpDate": "Apr 30, 2014 9:03:19 PM",
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
public class APIMigrateVmEvent extends APIEvent {
    /**
     * @desc see :ref:`VmInstanceInventory`
     */
    private VmInstanceInventory inventory;

    public APIMigrateVmEvent(String apiId) {
        super(apiId);
    }

    public APIMigrateVmEvent() {
        super(null);
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIMigrateVmEvent __example__() {
        APIMigrateVmEvent event = new APIMigrateVmEvent();

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
