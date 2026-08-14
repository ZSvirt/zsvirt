package org.zstack.header.managementnode;

import org.zstack.header.message.LocalEvent;

public class ManagementNodeLeftEvent extends LocalEvent {
    private String leftNodeId;
    private String sponsorNodeId;
    private boolean isHeartbeatStop;

    public ManagementNodeLeftEvent() {
    }

    public ManagementNodeLeftEvent(String leftNodeId, String sponsorNodeId, boolean isHeartbeatStop) {
        super();
        this.leftNodeId = leftNodeId;
        this.sponsorNodeId = sponsorNodeId;
        this.isHeartbeatStop = isHeartbeatStop;
    }

    @Override
    public String getSubCategory() {
        return ManagementNodeConstant.MANAGEMENT_NODE_EVENT;
    }

    public String getLeftNodeId() {
        return leftNodeId;
    }

    public void setLeftNodeId(String leftNodeId) {
        this.leftNodeId = leftNodeId;
    }

    public String getSponsorNodeId() {
        return sponsorNodeId;
    }

    public void setSponsorNodeId(String sponsorNodeId) {
        this.sponsorNodeId = sponsorNodeId;
    }

    public boolean isHeartbeatStop() {
        return isHeartbeatStop;
    }

    public void setHeartbeatStop(boolean isHeartbeatStop) {
        this.isHeartbeatStop = isHeartbeatStop;
    }
}
