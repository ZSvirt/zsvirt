package org.zstack.network.service.vip;

import org.zstack.header.message.MessageReply;

/**
 * Created by xing5 on 2016/11/30.
 */
public class ModifyVipAttributesReply extends MessageReply {
    private ModifyVipAttributesStruct struct;

    public ModifyVipAttributesStruct getStruct() {
        return struct;
    }

    public void setStruct(ModifyVipAttributesStruct struct) {
        this.struct = struct;
    }
}
