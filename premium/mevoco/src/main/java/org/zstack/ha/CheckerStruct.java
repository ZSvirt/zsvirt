package org.zstack.ha;

import org.zstack.header.vm.VmInstanceInventory;

/**
 * Created by xing5 on 2016/3/28.
 */
public class CheckerStruct {
    private String hostUuid;
    private VmInstanceInventory vmInstance;
    private long interval;
    private int maxTimes;
    private long successInterval;
    private int successTimes;
    private String hostIp;

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public long getSuccessInterval() {
        return successInterval;
    }

    public void setSuccessInterval(long successInterval) {
        this.successInterval = successInterval;
    }

    public int getSuccessTimes() {
        return successTimes;
    }

    public void setSuccessTimes(int successTimes) {
        this.successTimes = successTimes;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public VmInstanceInventory getVmInstance() {
        return vmInstance;
    }

    public void setVmInstance(VmInstanceInventory vmInstance) {
        this.vmInstance = vmInstance;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public int getMaxTimes() {
        return maxTimes;
    }

    public void setMaxTimes(int maxTimes) {
        this.maxTimes = maxTimes;
    }
}
