package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.message.MessageReply;

/**
 * Created by weiwang on 10/03/2017.
 */
public class AllocateVniReply extends MessageReply {
    private Integer vni;
    private String l2NetworkUuid;

    public Integer getVni() {
        return vni;
    }

    public void setVni(Integer vni) {
        this.vni = vni;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }
}
