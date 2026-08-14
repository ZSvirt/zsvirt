package org.zstack.kvm;

import org.zstack.header.host.AddHostMessage;

public interface AddKVMHostMessage extends AddHostMessage {
    String getUsername();
    String getPassword();
    int getSshPort();
}
