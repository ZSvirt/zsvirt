package org.zstack.compute.vm;

import org.zstack.header.message.Message;

import java.util.List;

/**
 * Created by MaJin on 2020/10/10.
 */
public interface VmInstanceExtensionManager {
    void handleMessage(Message msg);

    List<Class<? extends Message>> getMessageClasses();
}
