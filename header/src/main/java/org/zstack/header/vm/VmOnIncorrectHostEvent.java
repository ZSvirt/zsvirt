package org.zstack.header.vm;

import org.zstack.header.message.LocalEvent;

public class VmOnIncorrectHostEvent extends LocalEvent {
    private String vmInstanceUuid;
    private String hostUuidVmOn;
    private String hostUuidInDb;
    private boolean isStoppedVm;

    @Override
    public String getSubCategory() {
        return "VmOnIncorrectHostEvent";
    }

    public String getVmUuid() {
        return vmInstanceUuid;
    }

    public void setVmUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getHostUuidVmOn() {
        return hostUuidVmOn;
    }

    public void setHostUuidVmOn(String hostUuidVmOn) {
        this.hostUuidVmOn = hostUuidVmOn;
    }

    public String getHostUuidInDb() {
        return hostUuidInDb;
    }

    public void setHostUuidInDb(String hostUuidInDb) {
        this.hostUuidInDb = hostUuidInDb;
    }

    public boolean isStoppedVm() {
        return isStoppedVm;
    }

    public void setStoppedVm(boolean isStoppedVm) {
        this.isStoppedVm = isStoppedVm;
    }
}
