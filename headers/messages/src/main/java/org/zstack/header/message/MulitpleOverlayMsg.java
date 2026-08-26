package org.zstack.header.message;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.log.NoLogging;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;

public class MulitpleOverlayMsg extends NeedReplyMessage {
    @NoLogging(behavior = NoLogging.Behavior.Auto)
    protected List<Object> messages;
    protected String messageClassName;

    public void setMessages(List<NeedReplyMessage> msgs) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.addAll(msgs);
        messageClassName = msgs.get(0).getClass().getName();
    }

    public List<NeedReplyMessage> getMessages() {
        try {
            Class clazz = Class.forName(messageClassName);
            List<NeedReplyMessage> msgs = new ArrayList<>();
            messages.forEach(msg -> {
                NeedReplyMessage nmsg = (NeedReplyMessage) JSONObjectUtil.rehashObject(msg, clazz);
                if (nmsg instanceof OverlayMessage) {
                    OverlayMessage omsg = (OverlayMessage)nmsg;
                    omsg.setMessage(omsg.getMessage());
                    msgs.add(omsg);
                } else {
                    msgs.add(nmsg);
                }
            });
            return msgs;
        } catch (ClassNotFoundException e) {
            throw new CloudRuntimeException(e);
        }
    }
}
