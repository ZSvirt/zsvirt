package org.zstack.header.vm;

import org.zstack.header.allocator.AllocationScene;
import org.zstack.header.message.NeedQuotaCheckMessage;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:51 PM
 * To change this template use File | Settings | File Templates.
 */
@SkipVmTracer(replyClass = StartVmInstanceReply.class)
public class StartVmInstanceMsg extends NeedReplyMessage implements VmInstanceMessage, NeedQuotaCheckMessage, CheckAttachedVolumesMessage {
    private String vmInstanceUuid;
    private String accountUuid;
    private String hostUuid;
    private String clusterUuid;
    private List<String> softAvoidHostUuids;
    private AllocationScene allocationScene;
    private boolean startPaused;

    public boolean isStartPaused() {
        return startPaused;
    }

    public void setStartPaused(boolean startPaused) {
        this.startPaused = startPaused;
    }

    private List<String> avoidHostUuids;

    public AllocationScene getAllocationScene() {
        return allocationScene;
    }

    public void setAllocationScene(AllocationScene allocationScene) {
        this.allocationScene = allocationScene;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<String> getSoftAvoidHostUuids() {
        return softAvoidHostUuids;
    }

    public void setSoftAvoidHostUuids(List<String> softAvoidHostUuids) {
        this.softAvoidHostUuids = softAvoidHostUuids;
    }

    public List<String> getAvoidHostUuids() {
        return avoidHostUuids;
    }

    public void setAvoidHostUuids(List<String> avoidHostUuids) {
        this.avoidHostUuids = avoidHostUuids;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }
}
