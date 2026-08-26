package org.zstack.sdk.external.service;

import org.zstack.sdk.external.service.ExternalServiceCapabilities;

public class ExternalServiceInventory  {

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String status;
    public void setStatus(java.lang.String status) {
        this.status = status;
    }
    public java.lang.String getStatus() {
        return this.status;
    }

    public ExternalServiceCapabilities capabilities;
    public void setCapabilities(ExternalServiceCapabilities capabilities) {
        this.capabilities = capabilities;
    }
    public ExternalServiceCapabilities getCapabilities() {
        return this.capabilities;
    }

    public java.lang.String serviceType;
    public void setServiceType(java.lang.String serviceType) {
        this.serviceType = serviceType;
    }
    public java.lang.String getServiceType() {
        return this.serviceType;
    }

}
