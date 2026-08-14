package org.zstack.header.affinitygroup;

import org.zstack.header.message.MessageReply;

/**
 * Created by shixin on 2019-05-11.
 */
public class CreateAffinityGroupReply extends MessageReply {
    AffinityGroupInventory affinityGroup;

    public AffinityGroupInventory getAffinityGroup() {
        return affinityGroup;
    }

    public void setAffinityGroup(AffinityGroupInventory affinityGroup) {
        this.affinityGroup = affinityGroup;
    }
}
