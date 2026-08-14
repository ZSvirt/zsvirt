package org.zstack.kvm;

import org.zstack.kvm.KVMAgentCommands.AttachNicCommand;

/**
 * Created by xing5 on 2016/5/26.
 */
public interface KvmPreAttachNicExtensionPoint {
    void preAttachNicExtensionPoint(KVMHostInventory host, AttachNicCommand cmd);
}
