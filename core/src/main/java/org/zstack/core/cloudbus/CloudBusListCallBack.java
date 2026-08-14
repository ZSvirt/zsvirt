package org.zstack.core.cloudbus;

import org.zstack.header.core.AbstractCompletion;
import org.zstack.header.core.AsyncBackup;
import org.zstack.header.message.MessageReply;

import java.util.List;

public abstract class CloudBusListCallBack extends AbstractCompletion {
    public CloudBusListCallBack(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }

    public abstract void run(List<MessageReply> replies);
}
