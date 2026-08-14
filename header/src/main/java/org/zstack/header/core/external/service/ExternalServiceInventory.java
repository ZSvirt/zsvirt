package org.zstack.header.core.external.service;

public class ExternalServiceInventory {
    private String name;
    private String status;
    private ExternalServiceCapabilities capabilities;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ExternalServiceCapabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(ExternalServiceCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    public static ExternalServiceInventory __example__() {
        ExternalServiceInventory inv = new ExternalServiceInventory();
        inv.setName("prometheus");
        inv.setStatus(ExternalServiceStatus.RUNNING.toString());
        ExternalServiceCapabilities cap = new ExternalServiceCapabilities();
        cap.setReloadConfig(true);
        inv.setCapabilities(cap);
        return inv;
    }
}
