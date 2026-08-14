package org.zstack.vpc;

import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.appliancevm.ApplianceVmStatus;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeState;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.vpc.VpcConstants;
import org.zstack.header.vpc.VpcRouterDnsInventory;
import org.zstack.header.vpc.VpcRouterVmInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalProperty;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;

import static java.util.Arrays.asList;


@RestResponse(allTo = "inventory")
public class APIAddDnsToVpcRouterEvent extends APIEvent {
    /**
     * @desc see :ref:`VpcRouterVmInventory`
     */
    private VpcRouterVmInventory inventory;

    public APIAddDnsToVpcRouterEvent(String apiId) {
        super(apiId);
    }

    public APIAddDnsToVpcRouterEvent() {
        super(null);
    }

    public VpcRouterVmInventory getInventory() {
        return inventory;
    }

    public void setInventory(VpcRouterVmInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAddDnsToVpcRouterEvent __example__() {
        APIAddDnsToVpcRouterEvent event = new APIAddDnsToVpcRouterEvent();
        VpcRouterVmInventory vpcRouter = new VpcRouterVmInventory();

        String defaultL3Uuid = uuid();
        String rootVolumeUuid = uuid();

        vpcRouter.setName("TestVPC");
        vpcRouter.setDescription("this is a vpc for test");
        vpcRouter.setUuid(uuid());
        vpcRouter.setAllocatorStrategy(HostAllocatorConstant.LAST_HOST_PREFERRED_ALLOCATOR_STRATEGY_TYPE);
        vpcRouter.setClusterUuid(uuid());
        vpcRouter.setCpuNum(4);
        vpcRouter.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vpcRouter.setDefaultL3NetworkUuid(uuid());
        vpcRouter.setHostUuid(uuid());
        vpcRouter.setHypervisorType("KVM");
        vpcRouter.setImageUuid(uuid());
        vpcRouter.setInstanceOfferingUuid(uuid());
        vpcRouter.setLastHostUuid(uuid());
        vpcRouter.setMemorySize(SizeUnit.GIGABYTE.toByte(8));
        vpcRouter.setPlatform("Linux");
        vpcRouter.setRootVolumeUuid(rootVolumeUuid);
        vpcRouter.setState(VmInstanceState.Running.toString());
        vpcRouter.setType(ApplianceVmConstant.APPLIANCE_VM_TYPE);
        vpcRouter.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vpcRouter.setZoneUuid(uuid());
        vpcRouter.setStatus(ApplianceVmStatus.Connected.toString());
        vpcRouter.setAgentPort(VirtualRouterGlobalProperty.AGENT_PORT);
        vpcRouter.setManagementNetworkUuid(uuid());
        vpcRouter.setApplianceVmType(VpcConstants.VPC_VROUTER_VM_TYPE);

        VolumeInventory vol = new VolumeInventory();
        vol.setName(String.format("Root-Volume-For-VM-%s", vpcRouter.getUuid()));
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
        vol.setVmInstanceUuid(vpcRouter.getUuid());
        vol.setRootImageUuid(vpcRouter.getImageUuid());
        vpcRouter.setAllVolumes(asList(vol));

        VmNicInventory nic = new VmNicInventory();
        nic.setVmInstanceUuid(vpcRouter.getUuid());
        nic.setCreateDate(vpcRouter.getCreateDate());
        nic.setLastOpDate(vpcRouter.getLastOpDate());
        nic.setDeviceId(0);
        nic.setGateway("192.168.1.1");
        nic.setIp("192.168.1.10");
        nic.setL3NetworkUuid(defaultL3Uuid);
        nic.setNetmask("255.255.255.0");
        nic.setMac("00:0c:29:bd:99:fc");
        nic.setHypervisorType("KVM");
        nic.setUsedIpUuid(uuid());
        nic.setUuid(uuid());
        vpcRouter.setVmNics(asList(nic));

        VpcRouterDnsInventory dns = new VpcRouterDnsInventory();
        dns.setVpcRouterUuid(vpcRouter.getUuid());
        dns.setDns("8.8.8.8");
        vpcRouter.setDns(asList(dns));

        event.setInventory(vpcRouter);
        return event;
    }

}
