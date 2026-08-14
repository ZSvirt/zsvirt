package org.zstack.drs.message;

import org.zstack.drs.DRSMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by lining on 2019/12/13.
 */
public class CreateDRSVmMigrationActivityMsg extends NeedReplyMessage implements DRSMessage {
    private String activityUuid;

    private String drsUuid;

    private String vmUuid;

    private String vmSourceHostUuid;

    private String vmTargetHostUuid;

    private String reason;

    private String adviceUuid;

    public String getDrsUuid() {
        return drsUuid;
    }

    public void setDrsUuid(String drsUuid) {
        this.drsUuid = drsUuid;
    }

    public String getActivityUuid() {
        return activityUuid;
    }

    public void setActivityUuid(String activityUuid) {
        this.activityUuid = activityUuid;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getVmSourceHostUuid() {
        return vmSourceHostUuid;
    }

    public void setVmSourceHostUuid(String vmSourceHostUuid) {
        this.vmSourceHostUuid = vmSourceHostUuid;
    }

    public String getVmTargetHostUuid() {
        return vmTargetHostUuid;
    }

    public void setVmTargetHostUuid(String vmTargetHostUuid) {
        this.vmTargetHostUuid = vmTargetHostUuid;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getAdviceUuid() {
        return adviceUuid;
    }

    public void setAdviceUuid(String adviceUuid) {
        this.adviceUuid = adviceUuid;
    }

    @Override
    public String getDRSUuid() {
        return drsUuid;
    }
}
