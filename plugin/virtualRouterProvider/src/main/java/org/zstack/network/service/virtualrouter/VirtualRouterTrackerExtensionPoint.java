package org.zstack.network.service.virtualrouter;

import org.zstack.header.message.MessageReply;

/**
 * @author: zhanyong.miao
 * @date: 2019-08-21
 **/
public interface VirtualRouterTrackerExtensionPoint {
    void handleTracerReply(String resourceUuid, MessageReply reply);
}
