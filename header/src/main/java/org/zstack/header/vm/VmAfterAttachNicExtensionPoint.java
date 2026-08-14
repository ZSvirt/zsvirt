package org.zstack.header.vm;

import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;

/**
 * Created by LiangHanYu on 2022/4/13 13:24
 */
public interface VmAfterAttachNicExtensionPoint {
    void afterAttachNic(String nicUuid, VmInstanceInventory vmInstanceInventory, Completion completion);

    void afterAttachNicRollback(String nicUuid, VmInstanceInventory vmInstanceInventory, NoErrorCompletion completion);
}
