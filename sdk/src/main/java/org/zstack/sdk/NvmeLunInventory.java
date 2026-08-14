package org.zstack.sdk;



public class NvmeLunInventory extends org.zstack.sdk.LunInventory {

    public java.lang.String nvmeTargetUuid;
    public void setNvmeTargetUuid(java.lang.String nvmeTargetUuid) {
        this.nvmeTargetUuid = nvmeTargetUuid;
    }
    public java.lang.String getNvmeTargetUuid() {
        return this.nvmeTargetUuid;
    }

    public java.util.List nvmeLunHostRefs;
    public void setNvmeLunHostRefs(java.util.List nvmeLunHostRefs) {
        this.nvmeLunHostRefs = nvmeLunHostRefs;
    }
    public java.util.List getNvmeLunHostRefs() {
        return this.nvmeLunHostRefs;
    }

}
