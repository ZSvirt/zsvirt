package org.zstack.network.service.lb;

import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;

import java.util.List;

/**
 * @author: zhanyong.miao
 * @date: 2019-12-26
 **/
public class LoadBalancerGetPeerL3NetworksReply extends MessageReply {
    private List<L3NetworkInventory> inventories;

    public List<L3NetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L3NetworkInventory> inventories) {
        this.inventories = inventories;
    }
}
