package org.zstack.network.securitygroup;

import java.util.List;

/**
 * Created by LiangHanYu on 2022/3/7 21:01
 */
public interface AddSecurityGroupRuleMessage {
    String getSecurityGroupUuid();

    List<APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO> getRules();

    Integer getPriority();
}
