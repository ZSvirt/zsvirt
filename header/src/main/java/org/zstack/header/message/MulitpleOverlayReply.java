package org.zstack.header.message;

import java.util.ArrayList;
import java.util.List;

public class MulitpleOverlayReply extends MessageReply {
    private List<MessageReply> innerReplies;

    public List<MessageReply> getInnerReplies() {
        return innerReplies;
    }

    public void setInnerReplies(List<MessageReply> replies) {
        if (innerReplies == null) {
            innerReplies = new ArrayList<>();
        }
        innerReplies.addAll(replies);
    }
}
