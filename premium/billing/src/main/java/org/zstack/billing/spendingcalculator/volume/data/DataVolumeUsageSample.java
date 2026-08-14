package org.zstack.billing.spendingcalculator.volume.data;

import org.zstack.billing.UsageSample;

/**
 * Created by xing5 on 2016/6/11.
 */
public class DataVolumeUsageSample extends UsageSample {
    private String volumeUuid;
    private String volumeName;
    private String hypervisorType;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }
}
