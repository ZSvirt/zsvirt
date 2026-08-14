package org.zstack.ha;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * @Author: DaoDao
 * @Date: 2023/5/11
 */
public class RemoveVmFencerRuleFromHostMsg extends NeedReplyMessage {
    private String hostUuid;
    private List<VmRuleAttachFencer> allowRules;
    private List<VmRuleAttachFencer> blockRules;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<VmRuleAttachFencer> getAllowRules() {
        return allowRules;
    }

    public void setAllowRules(List<VmRuleAttachFencer> allowRules) {
        this.allowRules = allowRules;
    }

    public List<VmRuleAttachFencer> getBlockRules() {
        return blockRules;
    }

    public void setBlockRules(List<VmRuleAttachFencer> blockRules) {
        this.blockRules = blockRules;
    }
}
