package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.l2.L2NetworkMessage;

/**
 * Created by weiwang on 10/03/2017.
 */
public class AllocateVniMsg extends NeedReplyMessage implements L2NetworkMessage, VniAllocateMessage {
    private String allocateStrategy;
    private String l2NetworkUuid;
    private Integer requiredVni;

    public Integer getRequiredVni() {
        return requiredVni;
    }

    public void setRequiredVni(Integer requiredVni) {
        this.requiredVni = requiredVni;
    }

    public String getAllocateStrategy() {
        return allocateStrategy;
    }

    public String getAllocatorStrategy() {
        return allocateStrategy;
    }


    public void setAllocateStrategy(String allocateStrategy) {
        this.allocateStrategy = allocateStrategy;
    }

    @Override
    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }
}
