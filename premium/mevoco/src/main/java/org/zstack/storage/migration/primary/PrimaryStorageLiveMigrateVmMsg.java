package org.zstack.storage.migration.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.SkipVmTracer;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.storage.migration.PrimaryStorageMigrateVmContext;
import org.zstack.storage.migration.StorageMigrationMessage;

@SkipVmTracer(replyClass = PrimaryStorageLiveMigrateVmReply.class)
public class PrimaryStorageLiveMigrateVmMsg extends NeedReplyMessage implements StorageMigrationMessage, VmInstanceMessage {
    private String vmInstanceUuid;
    private String dstPrimaryStorageUuid;
    private String dstHostUuid;
    private boolean withDataVolumes;
    private String strategy;
    private Integer downTime;
    private PrimaryStorageMigrateVmContext jobContext;
    private String longJobUuid;
    private long bandwidth;

    public PrimaryStorageLiveMigrateVmMsg(PrimaryStorageMigrateVmMsg amsg) {
        this.vmInstanceUuid = amsg.getVmInstanceUuid();
        this.dstPrimaryStorageUuid = amsg.getDstPrimaryStorageUuid();
        this.dstHostUuid = amsg.getDstHostUuid();
        this.withDataVolumes = amsg.isWithDataVolumes();
        this.systemTags = amsg.getSystemTags();
        this.strategy = amsg.getStrategy();
        this.downTime = amsg.getDownTime();
        this.jobContext = amsg.getJobContext();
        this.longJobUuid = amsg.getLongJobUuid();
        this.bandwidth = amsg.getBandwidth();
    }

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

    public String getDstHostUuid() { return dstHostUuid; }

    public void setDstHostUuid(String dstHostUuid) {
        this.dstHostUuid = dstHostUuid;
    }

    public String getDstPrimaryStorageUuid() {
        return dstPrimaryStorageUuid;
    }

    public void setDstPrimaryStorageUuid(String dstPrimaryStorageUuid) {
        this.dstPrimaryStorageUuid = dstPrimaryStorageUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public boolean isWithDataVolumes() {
        return withDataVolumes;
    }

    public void setWithDataVolumes(boolean withDataVolumes) {
        this.withDataVolumes = withDataVolumes;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public Integer getDownTime() {
        return downTime;
    }

    public void setDownTime(Integer downTime) {
        this.downTime = downTime;
    }

    public long getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(long bandwidth) {
        this.bandwidth = bandwidth;
    }
}
