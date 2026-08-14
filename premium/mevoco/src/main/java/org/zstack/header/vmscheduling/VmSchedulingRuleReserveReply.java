package org.zstack.header.vmscheduling;

import org.zstack.header.message.MessageReply;

/**
 * @Author: DaoDao
 * @Date: 2022/11/30
 */
public class VmSchedulingRuleReserveReply extends MessageReply {
    String originHostUuid;

    public String getOriginHostUuid() {
        return originHostUuid;
    }

    public void setOriginHostUuid(String originHostUuid) {
        this.originHostUuid = originHostUuid;
    }
}
