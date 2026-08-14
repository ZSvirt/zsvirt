package org.zstack.header.storage.primary;

import org.zstack.header.core.Completion;
import org.zstack.header.message.Message;

public interface PrimaryStorage {
    void handleMessage(Message msg);

    void deleteHook();

    void changeStateHook(PrimaryStorageStateEvent evt, PrimaryStorageState nextState);

    void attachHook(String clusterUuid, Completion completion);

    void detachHook(String clusterUuid, Completion completion);
}
