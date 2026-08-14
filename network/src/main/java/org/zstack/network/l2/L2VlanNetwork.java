package org.zstack.network.l2;

import org.zstack.header.network.l2.*;
import org.zstack.network.service.NetworkServiceGlobalConfig;

public class L2VlanNetwork extends L2NoVlanNetwork {
    
    public L2VlanNetwork(L2NetworkVO self) {
        super(self);
    }


    public L2VlanNetwork() {
    }
    
    private L2VlanNetworkVO getSelf() {
        return (L2VlanNetworkVO) self;
    }

    @Override
    protected L2NetworkInventory getSelfInventory() {
        return L2VlanNetworkInventory.valueOf(getSelf());
    }

}
