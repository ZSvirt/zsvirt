package org.zstack.zmigrate.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;

import java.util.ArrayList;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIGetZMigrateGatewayVmInstancesReply extends APIReply {
    private String managementVmInstanceUuid;
    private List<VmInstanceInventory> gatewayVmInstances = new ArrayList<>();

    public String getManagementVmInstanceUuid() {
        return managementVmInstanceUuid;
    }

    public void setManagementVmInstanceUuid(String managementVmInstanceUuid) {
        this.managementVmInstanceUuid = managementVmInstanceUuid;
    }

    public List<VmInstanceInventory> getGatewayVmInstances() {
        return gatewayVmInstances;
    }

    public void setGatewayVmInstances(List<VmInstanceInventory> gatewayVmInstances) {
        this.gatewayVmInstances = gatewayVmInstances;
    }

    public static APIGetZMigrateGatewayVmInstancesReply __example__() {
        APIGetZMigrateGatewayVmInstancesReply reply = new APIGetZMigrateGatewayVmInstancesReply();
        reply.setManagementVmInstanceUuid(uuid(SoftwarePackageVO.class));
        List<VmInstanceInventory> gatewayVmInstances = new ArrayList<>();
        VmInstanceInventory vmInstance = new VmInstanceInventory();
        vmInstance.setUuid(uuid(VmInstanceVO.class));
        vmInstance.setName("ZMigrateGatewayVmInstance");
        vmInstance.setDescription("ZMigrate gateway vm instance for test");
        gatewayVmInstances.add(vmInstance);
        reply.setGatewayVmInstances(gatewayVmInstances);
        return reply;
    }
}