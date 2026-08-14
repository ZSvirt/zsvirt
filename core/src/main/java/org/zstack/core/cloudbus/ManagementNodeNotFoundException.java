package org.zstack.core.cloudbus;

import org.zstack.header.exception.CloudRuntimeException;

public class ManagementNodeNotFoundException extends CloudRuntimeException {
    private String managementNodeUuid;

    public ManagementNodeNotFoundException(String managementNodeUuid) {
        super(String.format("cannot find management node[uuid:%s]", managementNodeUuid));
        this.managementNodeUuid = managementNodeUuid;
    }

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }
}
