package org.zstack.network.securitygroup;

import org.zstack.header.message.Message;

public interface SecurityGroup {
    void handleMessage(Message msg);
}
