package org.zstack.billing;

import org.zstack.kvm.KVMConstant;

/**
 * Created by xing5 on 2016/3/5.
 */
public class SpendingDetails {
    public String resourceUuid;
    public String resourceName;
    public double spending;
    public String hypervisorType;
    public String type;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public double getSpending() {
        return spending;
    }

    public void setSpending(double spending) {
        this.spending = spending;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHypervisorType() {
        return hypervisorType == null ? KVMConstant.KVM_HYPERVISOR_TYPE : hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }
}
