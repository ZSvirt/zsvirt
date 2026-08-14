package org.zstack.network.securitygroup;

import org.zstack.header.message.Message;

public class RefreshSecurityGroupMsg extends Message implements SecurityGroupMessage {
    public static String OP_APPLY = "apply";
    public static String OP_REVOKE = "revoke";
    
    private String securityGroupUuid;
    private String vmInstanceUuid;
    private String operation;
    
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    @Override
    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }
}
