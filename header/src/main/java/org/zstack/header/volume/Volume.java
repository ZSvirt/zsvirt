package org.zstack.header.volume;

import org.zstack.header.message.Message;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:08 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Volume {
    void handleMessage(Message msg);
}
