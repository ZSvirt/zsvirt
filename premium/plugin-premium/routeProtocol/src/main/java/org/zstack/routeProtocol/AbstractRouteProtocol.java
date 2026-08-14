package org.zstack.routeProtocol;

import org.zstack.header.message.Message;
/**
 *
 */
public abstract class AbstractRouteProtocol{
    abstract void handleMessage(Message msg);
}
