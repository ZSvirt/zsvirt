package org.zstack.header.network.service;

import org.zstack.header.core.Completion;

public interface VirtualRouterHaCallbackInterface {
    void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion);
}
