package org.zstack.image;

import org.zstack.header.message.Message;

import java.util.List;

public interface ImageExtensionManager {
    void handleMessage(Message msg);

    List<Class> getMessageClasses();
}
