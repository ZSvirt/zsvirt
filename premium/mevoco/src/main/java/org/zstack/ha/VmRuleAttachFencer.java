package org.zstack.ha;

import java.util.List;

/**
 * @Author: DaoDao
 * @Date: 2023/5/11
 */
public class VmRuleAttachFencer {
    private String fencerName;
    private List<String> vmUuids;

    public String getFencerName() {
        return fencerName;
    }

    public void setFencerName(String fencerName) {
        this.fencerName = fencerName;
    }

    public List<String> getVmUuids() {
        return vmUuids;
    }

    public void setVmUuids(List<String> vmUuids) {
        this.vmUuids = vmUuids;
    }
}
