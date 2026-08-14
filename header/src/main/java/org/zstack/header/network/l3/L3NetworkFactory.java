package org.zstack.header.network.l3;

import org.zstack.header.core.ReturnValueCompletion;

public interface L3NetworkFactory {
    L3NetworkType getType();

    void createL3Network(L3NetworkVO l3vo, APICreateL3NetworkMsg msg, ReturnValueCompletion<L3NetworkInventory> completion);

    L3Network getL3Network(L3NetworkVO vo);

    boolean applyNetworkServiceWhenVmStateChange();
}
