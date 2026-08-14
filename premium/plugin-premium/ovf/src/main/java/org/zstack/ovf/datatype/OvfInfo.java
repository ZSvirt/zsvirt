package org.zstack.ovf.datatype;

import org.zstack.header.rest.APINoSee;

import java.util.List;

/**
 * Created by Qi Le on 2022/3/4
 */
public class OvfInfo {
    private List<OvfDiskInfo> disks;
    private List<OvfNetworkInfo> networks;
    private OvfCpuInfo cpu;
    private OvfMemoryInfo memory;
    private String vmName;
    private OvfOSInfo os;
    private OvfSystemInfo systemInfo;
    private List<OvfEthernetAdapterInfo> nics;
    private List<OvfCdDriverInfo> cdDrivers;
    private List<OvfVolumeInfo> volumes;

    /**
     * Note: If you want to add new field which ovf file not contains,
     * you should add them in OvfPreAnalysisInfo.
     */
    @APINoSee
    private OvfPreAnalysisInfo preAnalysisInfo;

    public List<OvfVolumeInfo> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<OvfVolumeInfo> volumes) {
        this.volumes = volumes;
    }

    public List<OvfCdDriverInfo> getCdDrivers() {
        return cdDrivers;
    }

    public void setCdDrivers(List<OvfCdDriverInfo> cdDrivers) {
        this.cdDrivers = cdDrivers;
    }

    public List<OvfEthernetAdapterInfo> getNics() {
        return nics;
    }

    public void setNics(List<OvfEthernetAdapterInfo> nics) {
        this.nics = nics;
    }

    public OvfCpuInfo getCpu() {
        return cpu;
    }

    public void setCpu(OvfCpuInfo cpu) {
        this.cpu = cpu;
    }

    public OvfMemoryInfo getMemory() {
        return memory;
    }

    public void setMemory(OvfMemoryInfo memory) {
        this.memory = memory;
    }

    public OvfSystemInfo getSystemInfo() {
        return systemInfo;
    }

    public void setSystemInfo(OvfSystemInfo systemInfo) {
        this.systemInfo = systemInfo;
    }

    public OvfOSInfo getOs() {
        return os;
    }

    public void setOs(OvfOSInfo os) {
        this.os = os;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public List<OvfDiskInfo> getDisks() {
        return disks;
    }

    public void setDisks(List<OvfDiskInfo> disks) {
        this.disks = disks;
    }

    public List<OvfNetworkInfo> getNetworks() {
        return networks;
    }

    public void setNetworks(List<OvfNetworkInfo> networks) {
        this.networks = networks;
    }

    public OvfPreAnalysisInfo getPreAnalysisInfo() {
        return preAnalysisInfo;
    }

    public void setPreAnalysisInfo(OvfPreAnalysisInfo preAnalysisInfo) {
        this.preAnalysisInfo = preAnalysisInfo;
    }
}
