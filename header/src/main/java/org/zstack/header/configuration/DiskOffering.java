package org.zstack.header.configuration;

import org.zstack.header.message.Message;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 2:41 PM
 * To change this template use File | Settings | File Templates.
 */
public interface DiskOffering {
    void handleMessage(Message msg);
}
