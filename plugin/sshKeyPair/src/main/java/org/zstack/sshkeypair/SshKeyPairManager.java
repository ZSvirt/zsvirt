package org.zstack.sshkeypair;

import org.zstack.header.message.Message;

public interface SshKeyPairManager {
    String SERVICE_ID = "sshKeyPair";
    void handleMessage(Message msg);
}
