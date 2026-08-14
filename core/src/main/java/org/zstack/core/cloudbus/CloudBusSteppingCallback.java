package org.zstack.core.cloudbus;

import org.zstack.header.core.AbstractCompletion;
import org.zstack.header.core.AsyncBackup;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;

/**
 */
public abstract class CloudBusSteppingCallback extends AbstractCompletion {
    public CloudBusSteppingCallback(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }

    public abstract void run(NeedReplyMessage msg, MessageReply reply);
}
