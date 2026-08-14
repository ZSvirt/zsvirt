package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:46 PM
 * To change this template use File | Settings | File Templates.
 */
@SkipVmTracer(replyClass = StopVmInstanceReply.class)
public class StopVmInstanceMsg extends NeedReplyMessage implements VmInstanceMessage, StopVmMessage, ReleaseResourceMessage {
    private String vmInstanceUuid;
    private boolean gcOnFailure;
    private String type = StopVmType.grace.toString();
    private boolean ignoreResourceReleaseFailure;

    /**
     * @since zsv 4.2.6
     */
    private boolean stopHA;

    public boolean isGcOnFailure() {
        return gcOnFailure;
    }

    public void setGcOnFailure(boolean gcOnFailure) {
        this.gcOnFailure = gcOnFailure;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public boolean isIgnoreResourceReleaseFailure() {
        return ignoreResourceReleaseFailure;
    }

    public void setIgnoreResourceReleaseFailure(boolean ignoreResourceReleaseFailure) {
        this.ignoreResourceReleaseFailure = ignoreResourceReleaseFailure;
    }

    public boolean isStopHA() {
        return stopHA;
    }

    public void setStopHA(boolean stopHA) {
        this.stopHA = stopHA;
    }
}
