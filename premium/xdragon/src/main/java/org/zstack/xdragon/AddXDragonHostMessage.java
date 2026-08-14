package org.zstack.xdragon;

import org.zstack.header.host.AddHostMessage;
import org.zstack.kvm.AddKVMHostMessage;

public interface AddXDragonHostMessage extends AddHostMessage, AddKVMHostMessage {
    Integer getCpuNum();
    Integer getCpuSockets();
    Long getTotalPhysicalMemory();
}
