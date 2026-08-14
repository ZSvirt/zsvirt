package org.zstack.header.apimediator;

import org.zstack.header.message.Message;

public interface ApiMessageRouter {
    String generateTargetServiceId(Message msg) throws CloudNoRouteFoundException;
}
