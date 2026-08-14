package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

/**
 * Created by mingjian.deng on 2018/4/19.
 */
public class CheckMountDomainReply extends MessageReply {
    Boolean active = false;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
