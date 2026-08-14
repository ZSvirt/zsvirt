package org.zstack.header.vmscheduling;

import org.zstack.header.affinitygroup.CreateAffinityGroupMsg;

/**
 * @Author: DaoDao
 * @Date: 2022/11/29
 */
public class CreateVmSchedulingRuleMsg extends CreateAffinityGroupMsg {
    private String rule;
    private String level;
    private String vmGroupUuid;
    private String hostGroupUuid;

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getVmGroupUuid() {
        return vmGroupUuid;
    }

    public void setVmGroupUuid(String vmGroupUuid) {
        this.vmGroupUuid = vmGroupUuid;
    }

    public String getHostGroupUuid() {
        return hostGroupUuid;
    }

    public void setHostGroupUuid(String hostGroupUuid) {
        this.hostGroupUuid = hostGroupUuid;
    }
}
