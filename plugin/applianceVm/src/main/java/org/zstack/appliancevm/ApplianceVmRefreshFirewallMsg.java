package org.zstack.appliancevm;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceMessage;

/**
 */
public class ApplianceVmRefreshFirewallMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private boolean inSyncThread;

    public boolean isInSyncThread() {
        return inSyncThread;
    }

    public void setInSyncThread(boolean inSyncThread) {
        this.inSyncThread = inSyncThread;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
