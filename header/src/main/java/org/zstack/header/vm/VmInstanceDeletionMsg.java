package org.zstack.header.vm;

import org.zstack.header.message.DeletionMessage;

/**
 */
public class VmInstanceDeletionMsg extends DeletionMessage implements VmInstanceMessage, ReleaseResourceMessage {
    private String vmInstanceUuid;
    private String deletionPolicy;
    private boolean additionalFlowRequested = true;
    private boolean ignoreResourceReleaseFailure = false;

    public String getDeletionPolicy() {
        return deletionPolicy;
    }

    public void setDeletionPolicy(String deletionPolicy) {
        this.deletionPolicy = deletionPolicy;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public boolean isIgnoreResourceReleaseFailure() {
        return ignoreResourceReleaseFailure;
    }

    public void setIgnoreResourceReleaseFailure(boolean ignoreResourceReleaseFailure) {
        this.ignoreResourceReleaseFailure = ignoreResourceReleaseFailure;
    }

    public boolean isAdditionalFlowRequested() {
        return additionalFlowRequested;
    }

    public void setAdditionalFlowRequested(boolean additionalFlowRequested) {
        this.additionalFlowRequested = additionalFlowRequested;
    }
}
