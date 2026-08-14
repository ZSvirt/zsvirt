package org.zstack.header.image;

import org.zstack.header.message.Message;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 5:37 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Image {
    void handleMessage(Message msg);
}
