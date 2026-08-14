package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by Wenhao.Zhang on 22/06/29
 */
public class SetVmQgaSyncClockTaskMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    /**
     * null if not set
     */
    private Boolean syncAfterVMResume;
    /**
     * interval for sync clock. null if not set
     */
    private Integer intervalInSeconds;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public Boolean getSyncAfterVMResume() {
        return syncAfterVMResume;
    }

    public void setSyncAfterVMResume(Boolean syncAfterVMResume) {
        this.syncAfterVMResume = syncAfterVMResume;
    }

    public Integer getIntervalInSeconds() {
        return intervalInSeconds;
    }

    public void setIntervalInSeconds(Integer intervalInSeconds) {
        this.intervalInSeconds = intervalInSeconds;
    }
}
