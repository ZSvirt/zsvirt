package org.zstack.core.cloudbus;

import org.zstack.header.message.Message;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public interface MarshalReplyMessageExtensionPoint {
    /**
     * @return a list of {@link org.zstack.header.message.MessageReply} or {@link org.zstack.header.message.Event}
     */
    List<Class<? extends Message>> getReplyMessageClassForMarshalExtensionPoint();

    /**
     * @param msg
     *   null if replyOrEvent is an {@link org.zstack.header.message.Event}
     */
    void marshalReplyMessageBeforeSending(Message replyOrEvent, NeedReplyMessage msg);

    default void marshalReplyMessageBeforeDropping(Message replyOrEvent, NeedReplyMessage msg) {

    }
}
