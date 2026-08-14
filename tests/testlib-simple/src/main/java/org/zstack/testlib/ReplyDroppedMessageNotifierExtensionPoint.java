package org.zstack.testlib;

import groovy.lang.Closure;
import org.zstack.core.cloudbus.MarshalReplyMessageExtensionPoint;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.getReflections;

public class ReplyDroppedMessageNotifierExtensionPoint implements MarshalReplyMessageExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ReplyDroppedMessageNotifierExtensionPoint.class);

    private static final List<Class> replyClasses = new ArrayList<>();

    static {
        replyClasses.addAll(getReflections().getSubTypesOf(MessageReply.class));
    }

    @Override
    public List<Class> getReplyMessageClassForMarshalExtensionPoint() {
        return replyClasses;
    }

    @Override
    public void marshalReplyMessageBeforeSending(Message replyOrEvent, NeedReplyMessage msg) {

    }

    private void handleMessageWithoutReply(NeedReplyMessage msg) {
        Test.getCurrentEnvSpec().messagesWithoutReplies.forEach((msgClz, cs) -> {
            if (msg.getClass() == msgClz) {
                logger.debug("class matched, execute closure " + cs.size());
                synchronized (cs) {
                    for (Closure c : cs) {
                        c.call(msg);
                    }
                }
            }
        });
    }

    @Override
    public void marshalReplyMessageBeforeDropping(Message replyOrEvent, NeedReplyMessage msg) {
        if (Test.getCurrentEnvSpec().notifiersOfReceivedMessages == null) {
            return;
        }

        if (replyOrEvent == null) {
            handleMessageWithoutReply(msg);
        } else {
            handleReplyMessage(replyOrEvent, msg);
        }
    }

    private void handleReplyMessage(Message replyOrEvent, NeedReplyMessage msg) {
        logger.debug(String.format("reply class is %s", replyOrEvent.getClass().toString()));
        Test.getCurrentEnvSpec().notifiersOfReceivedMessages.forEach((msgClz, cs) -> {
            if (replyOrEvent.getClass() == msgClz) {
                logger.debug("class matched, execute closure " + cs.size());
                synchronized (cs) {
                    for (Closure c : cs) {
                        c.call(replyOrEvent);
                    }
                }
            }
        });
    }
}
