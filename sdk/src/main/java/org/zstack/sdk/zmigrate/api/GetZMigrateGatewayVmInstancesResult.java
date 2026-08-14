package org.zstack.sdk.zmigrate.api;



public class GetZMigrateGatewayVmInstancesResult {
    public java.lang.String managementVmInstanceUuid;
    public void setManagementVmInstanceUuid(java.lang.String managementVmInstanceUuid) {
        this.managementVmInstanceUuid = managementVmInstanceUuid;
    }
    public java.lang.String getManagementVmInstanceUuid() {
        return this.managementVmInstanceUuid;
    }

    public java.util.List gatewayVmInstances;
    public void setGatewayVmInstances(java.util.List gatewayVmInstances) {
        this.gatewayVmInstances = gatewayVmInstances;
    }
    public java.util.List getGatewayVmInstances() {
        return this.gatewayVmInstances;
    }

}
