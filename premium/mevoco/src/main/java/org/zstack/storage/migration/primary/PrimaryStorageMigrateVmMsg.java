package org.zstack.storage.migration.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.SkipVmTracer;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.storage.migration.PrimaryStorageMigrateVmContext;
import org.zstack.storage.migration.PrimaryStorageMigrateVmMessage;

/**
 * Created by GuoYi on 12/7/17.
 * <p>
 * This message is for PrimaryStorageMigrateVolumeJob
 */

@SkipVmTracer(replyClass = PrimaryStorageMigrateVmReply.class)
public class PrimaryStorageMigrateVmMsg extends NeedReplyMessage implements PrimaryStorageMigrateVmMessage, VmInstanceMessage {
    private String vmInstanceUuid;

    private String dstPrimaryStorageUuid;

    private String dstHostUuid;

    private boolean withDataVolumes = true;

    private boolean withSnapshots = false;

    private String strategy;

    private Integer downTime;

    private PrimaryStorageMigrateVmContext jobContext;

    private String longJobUuid;

    private long bandwidth;

    public PrimaryStorageMigrateVmContext getJobContext() {
        return jobContext;
    }

    public void setJobContext(PrimaryStorageMigrateVmContext jobContext) {
        this.jobContext = jobContext;
    }

    public String getLongJobUuid() {
        return longJobUuid;
    }

    public void setLongJobUuid(String longJobUuid) {
        this.longJobUuid = longJobUuid;
    }

    public PrimaryStorageMigrateVmMsg() {
    }

    public PrimaryStorageMigrateVmMsg(APIPrimaryStorageMigrateVmMsg amsg) {
        vmInstanceUuid = amsg.getVmInstanceUuid();
        dstPrimaryStorageUuid = amsg.getDstPrimaryStorageUuid();
        dstHostUuid = amsg.getDstHostUuid();
        withDataVolumes = amsg.isWithDataVolumes();
        withSnapshots = amsg.isWithSnapshots();
        systemTags = amsg.getSystemTags();
        strategy = amsg.getStrategy();
        downTime = amsg.getDownTime();
        bandwidth = amsg.getBandwidth();
    }

    public Integer getDownTime() {
        return downTime;
    }

    public void setDownTime(Integer downTime) {
        this.downTime = downTime;
    }

    public String getDstHostUuid() { return dstHostUuid; }

    public void setDstHostUuid(String dstHostUuid) {
        this.dstHostUuid = dstHostUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getDstPrimaryStorageUuid() {
        return dstPrimaryStorageUuid;
    }

    public void setDstPrimaryStorageUuid(String dstPrimaryStorageUuid) {
        this.dstPrimaryStorageUuid = dstPrimaryStorageUuid;
    }

    public boolean isWithDataVolumes() {
        return withDataVolumes;
    }

    public void setWithDataVolumes(boolean withDataVolumes) {
        this.withDataVolumes = withDataVolumes;
    }

    public boolean isWithSnapshots() {
        return withSnapshots;
    }

    public void setWithSnapshots(boolean withSnapshots) {
        this.withSnapshots = withSnapshots;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public long getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(long bandwidth) {
        this.bandwidth = bandwidth;
    }
}
