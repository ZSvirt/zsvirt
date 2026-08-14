package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by camile on 3/7/2018.
 * copy by APIMigrateVmMsg for LongJob
 */
@SkipVmTracer(replyClass = MigrateVmInnerReply.class)
public class MigrateVmInnerMsg extends NeedReplyMessage implements VmInstanceMessage, MigrateVmMessage, CheckAttachedVolumesMessage {
    private String vmInstanceUuid;
    private String hostUuid;
    private String strategy;
    private Boolean migrateFromDestination;
    private boolean allowUnknown;
    private Integer downTime;

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public void setAllowUnknown(boolean allowUnknown) {
        this.allowUnknown = allowUnknown;
    }

    @Override
    public boolean isMigrateFromDestination() {
        return migrateFromDestination == null ? false : migrateFromDestination;
    }

    @Override
    public boolean isAllowUnknown() {
        return allowUnknown;
    }

    public void setMigrateFromDestination(Boolean migrateFromDestination) {
        this.migrateFromDestination = migrateFromDestination;
    }

    @Override
    public String getStrategy() {
        return strategy;
    }

    @Override
    public Integer getDownTime() {
        return null;
    }

    public void setDownTime(Integer downTime) {
        this.downTime = downTime;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
}
