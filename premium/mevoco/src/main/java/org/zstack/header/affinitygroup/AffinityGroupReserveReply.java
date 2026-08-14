package org.zstack.header.affinitygroup;

import org.zstack.header.message.MessageReply;

/**
 * Created by shixin on 2018-02-08.
 */
public class AffinityGroupReserveReply extends MessageReply {
    String originHostUuid;

    public String getOriginHostUuid() {
        return originHostUuid;
    }

    public void setOriginHostUuid(String originHostUuid) {
        this.originHostUuid = originHostUuid;
    }
}
