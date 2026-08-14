package org.zstack.header.storage.snapshot.group;

public class VmNicConflictEntry {
    private String ip;
    private String mac;
    private String vmNicName;
    private String vmInstanceName;
    private String vmInstanceUuid;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getVmNicName() {
        return vmNicName;
    }

    public void setVmNicName(String vmNicName) {
        this.vmNicName = vmNicName;
    }

    public String getVmInstanceName() {
        return vmInstanceName;
    }

    public void setVmInstanceName(String vmInstanceName) {
        this.vmInstanceName = vmInstanceName;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
