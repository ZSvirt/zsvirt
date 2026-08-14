package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by frank on 11/1/2015.
 */
public class VmStateChangedOnHostMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmStateAtTracingMoment;
    private String vmInstanceUuid;
    private String hostUuid;
    private String stateOnHost;
    //fromSync is used to determine whether the msg is from a host
    private boolean fromSync = false;

    public VmStateChangedOnHostMsg() { }

    public String getVmStateAtTracingMoment() {
        return vmStateAtTracingMoment;
    }

    public void setVmStateAtTracingMoment(VmInstanceState vmStateAtTracingMoment) {
        this.vmStateAtTracingMoment = vmStateAtTracingMoment == null ? null : vmStateAtTracingMoment.toString();
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getStateOnHost() {
        return stateOnHost;
    }

    public void setStateOnHost(VmInstanceState stateOnHost) {
        this.stateOnHost = stateOnHost.toString();
    }

    public boolean isFromSync() {
        return fromSync;
    }

    public void setFromSync(boolean fromSync) {
        this.fromSync = fromSync;
    }
}
