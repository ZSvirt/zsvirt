package org.zstack.sdk;

import org.zstack.sdk.OvfCpuInfo;
import org.zstack.sdk.OvfMemoryInfo;
import org.zstack.sdk.OvfOSInfo;
import org.zstack.sdk.OvfSystemInfo;

public class OvfInfo  {

    public java.util.List disks;
    public void setDisks(java.util.List disks) {
        this.disks = disks;
    }
    public java.util.List getDisks() {
        return this.disks;
    }

    public java.util.List networks;
    public void setNetworks(java.util.List networks) {
        this.networks = networks;
    }
    public java.util.List getNetworks() {
        return this.networks;
    }

    public OvfCpuInfo cpu;
    public void setCpu(OvfCpuInfo cpu) {
        this.cpu = cpu;
    }
    public OvfCpuInfo getCpu() {
        return this.cpu;
    }

    public OvfMemoryInfo memory;
    public void setMemory(OvfMemoryInfo memory) {
        this.memory = memory;
    }
    public OvfMemoryInfo getMemory() {
        return this.memory;
    }

    public java.lang.String vmName;
    public void setVmName(java.lang.String vmName) {
        this.vmName = vmName;
    }
    public java.lang.String getVmName() {
        return this.vmName;
    }

    public OvfOSInfo os;
    public void setOs(OvfOSInfo os) {
        this.os = os;
    }
    public OvfOSInfo getOs() {
        return this.os;
    }

    public OvfSystemInfo systemInfo;
    public void setSystemInfo(OvfSystemInfo systemInfo) {
        this.systemInfo = systemInfo;
    }
    public OvfSystemInfo getSystemInfo() {
        return this.systemInfo;
    }

    public java.util.List nics;
    public void setNics(java.util.List nics) {
        this.nics = nics;
    }
    public java.util.List getNics() {
        return this.nics;
    }

    public java.util.List cdDrivers;
    public void setCdDrivers(java.util.List cdDrivers) {
        this.cdDrivers = cdDrivers;
    }
    public java.util.List getCdDrivers() {
        return this.cdDrivers;
    }

    public java.util.List volumes;
    public void setVolumes(java.util.List volumes) {
        this.volumes = volumes;
    }
    public java.util.List getVolumes() {
        return this.volumes;
    }

}
