package org.zstack.header.network.l2;

import org.zstack.header.core.ReturnValueCompletion;

public interface L2NetworkFactory {
    L2NetworkType getType();

    void createL2Network(L2NetworkVO vo, APICreateL2NetworkMsg msg, ReturnValueCompletion<L2NetworkInventory> completion);

    L2Network getL2Network(L2NetworkVO vo);
}
