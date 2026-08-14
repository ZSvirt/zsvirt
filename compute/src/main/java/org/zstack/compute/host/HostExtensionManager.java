package org.zstack.compute.host;

import org.zstack.header.message.Message;

import java.util.List;

public interface HostExtensionManager {
    void handleMessage(Message msg);

    List<Class> getMessageClasses();
}
