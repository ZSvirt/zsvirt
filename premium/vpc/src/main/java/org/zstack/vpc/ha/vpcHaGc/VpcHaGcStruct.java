package org.zstack.vpc.ha.vpcHaGc;

import org.zstack.header.network.service.VirtualRouterHaTask;

public class VpcHaGcStruct {
    String vmInstanceUuid;
    String taskName;
    VirtualRouterHaTask taskData;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public VirtualRouterHaTask getTaskData() {
        return taskData;
    }

    public void setTaskData(VirtualRouterHaTask taskData) {
        this.taskData = taskData;
    }
}
