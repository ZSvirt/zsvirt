package org.zstack.kvm;

import org.zstack.kvm.KVMAgentCommands.DetachIsoCmd;

/**
 * Created by xing5 on 2016/5/27.
 */
public interface KVMPreDetachIsoExtensionPoint {
    void preDetachIsoExtensionPoint(KVMHostInventory host, DetachIsoCmd cmd);
}
