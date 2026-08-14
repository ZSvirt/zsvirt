package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by xing5 on 2016/3/29.
 */
@SkipVmTracer(replyClass = HaStartVmInstanceReply.class)
public class HaStartVmInstanceMsg extends NeedReplyMessage implements VmInstanceMessage, CheckAttachedVolumesMessage{
    private String vmInstanceUuid;
    private String judgerClassName;
    private List<String> softAvoidHostUuids;
    private String haReason;

    public String getJudgerClassName() {
        return judgerClassName;
    }

    public void setJudgerClassName(String judgerClassName) {
        this.judgerClassName = judgerClassName;
    }

    public List<String> getSoftAvoidHostUuids() {
        return softAvoidHostUuids;
    }

    public void setSoftAvoidHostUuids(List<String> softAvoidHostUuids) {
        this.softAvoidHostUuids = softAvoidHostUuids;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getHaReason() {
        return haReason;
    }

    public void setHaReason(String haReason) {
        this.haReason = haReason;
    }
}
