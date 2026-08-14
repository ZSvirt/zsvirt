package org.zstack.network.securitygroup;

import org.zstack.header.message.APIParam;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by LiangHanYu on 2022/3/7 10:12
 */
public class AddSecurityGroupRuleMsg extends NeedReplyMessage implements AddSecurityGroupRuleMessage {
    private String securityGroupUuid;
    private String type;
    private Integer priority = -1;
    /**
     * @desc a list of :ref:`SecurityGroupRuleAO` that describe rules
     */
    private List<APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO> rules;

    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }

    public List<APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO> getRules() {
        return rules;
    }

    public void setRules(List<APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO> rules) {
        this.rules = rules;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
