package org.zstack.drs.data;

/**
 * Created by lining on 2019/12/6.
 */
public class MigrateTask {
    public String vmUuid;
    public String sourceHostUuid;
    public String targetHostUuid;

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getSourceHostUuid() {
        return sourceHostUuid;
    }

    public void setSourceHostUuid(String sourceHostUuid) {
        this.sourceHostUuid = sourceHostUuid;
    }

    public String getTargetHostUuid() {
        return targetHostUuid;
    }

    public void setTargetHostUuid(String targetHostUuid) {
        this.targetHostUuid = targetHostUuid;
    }
}
