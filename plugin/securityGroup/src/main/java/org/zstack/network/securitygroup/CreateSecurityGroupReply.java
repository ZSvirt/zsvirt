package org.zstack.network.securitygroup;

import org.zstack.header.message.MessageReply;

/**
 * Created by LiangHanYu on 2022/3/4 16:11
 */
public class CreateSecurityGroupReply extends MessageReply {
    private SecurityGroupInventory inventory;

    public SecurityGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(SecurityGroupInventory inventory) {
        this.inventory = inventory;
    }
}
