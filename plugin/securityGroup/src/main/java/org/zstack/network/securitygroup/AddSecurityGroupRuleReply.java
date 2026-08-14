package org.zstack.network.securitygroup;

import org.zstack.header.message.MessageReply;

/**
 * Created by LiangHanYu on 2022/3/7 10:12
 */
public class AddSecurityGroupRuleReply extends MessageReply {
    private SecurityGroupInventory inventory;

    public SecurityGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(SecurityGroupInventory inventory) {
        this.inventory = inventory;
    }
}
