package org.zstack.kvm;

import org.zstack.kvm.KVMAgentCommands.AttachIsoCmd;

/**
 * Created by xing5 on 2016/5/27.
 */
public interface KVMPreAttachIsoExtensionPoint {
    void preAttachIsoExtensionPoint(KVMHostInventory host, AttachIsoCmd cmd);
}
