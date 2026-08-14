package org.zstack.sdk;



public class LoadBalancerListenerInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String loadBalancerUuid;
    public void setLoadBalancerUuid(java.lang.String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }
    public java.lang.String getLoadBalancerUuid() {
        return this.loadBalancerUuid;
    }

    public java.lang.Integer instancePort;
    public void setInstancePort(java.lang.Integer instancePort) {
        this.instancePort = instancePort;
    }
    public java.lang.Integer getInstancePort() {
        return this.instancePort;
    }

    public java.lang.Integer loadBalancerPort;
    public void setLoadBalancerPort(java.lang.Integer loadBalancerPort) {
        this.loadBalancerPort = loadBalancerPort;
    }
    public java.lang.Integer getLoadBalancerPort() {
        return this.loadBalancerPort;
    }

    public java.lang.String securityPolicyType;
    public void setSecurityPolicyType(java.lang.String securityPolicyType) {
        this.securityPolicyType = securityPolicyType;
    }
    public java.lang.String getSecurityPolicyType() {
        return this.securityPolicyType;
    }

    public java.lang.String protocol;
    public void setProtocol(java.lang.String protocol) {
        this.protocol = protocol;
    }
    public java.lang.String getProtocol() {
        return this.protocol;
    }

    public java.lang.String serverGroupUuid;
    public void setServerGroupUuid(java.lang.String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
    }
    public java.lang.String getServerGroupUuid() {
        return this.serverGroupUuid;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

    public java.util.List vmNicRefs;
    public void setVmNicRefs(java.util.List vmNicRefs) {
        this.vmNicRefs = vmNicRefs;
    }
    public java.util.List getVmNicRefs() {
        return this.vmNicRefs;
    }

    public java.util.List aclRefs;
    public void setAclRefs(java.util.List aclRefs) {
        this.aclRefs = aclRefs;
    }
    public java.util.List getAclRefs() {
        return this.aclRefs;
    }

    public java.util.List certificateRefs;
    public void setCertificateRefs(java.util.List certificateRefs) {
        this.certificateRefs = certificateRefs;
    }
    public java.util.List getCertificateRefs() {
        return this.certificateRefs;
    }

    public java.util.List serverGroupRefs;
    public void setServerGroupRefs(java.util.List serverGroupRefs) {
        this.serverGroupRefs = serverGroupRefs;
    }
    public java.util.List getServerGroupRefs() {
        return this.serverGroupRefs;
    }

}
