package org.zstack.core.cloudbus;

import org.zstack.header.core.AbstractCompletion;
import org.zstack.header.core.AsyncBackup;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;

public abstract class CloudBusCallBack extends AbstractCompletion {
    public CloudBusCallBack(AsyncBackup one, AsyncBackup...others) {
        super(one, others);
    }

	public abstract void run(MessageReply reply);
}
