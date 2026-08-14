package org.zstack.header.cluster;

import org.zstack.header.message.Message;

public interface Cluster {
    void handleMessage(Message msg);
}
