package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by frank on 7/18/2015.
 */
public class DetachNicFromVmMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private String vmNicUuid;
    private boolean haPeer = false;

    // do not call DetachNicFromVmOnHypervisorMsg even vm is running
    private boolean dbOnly = false;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public boolean isDbOnly() {
        return dbOnly;
    }

    public void setDbOnly(boolean dbOnly) {
        this.dbOnly = dbOnly;
    }

    public boolean isHaPeer() { return haPeer; }
    public void setHaPeer(boolean haPeer) { this.haPeer = haPeer; }
}
