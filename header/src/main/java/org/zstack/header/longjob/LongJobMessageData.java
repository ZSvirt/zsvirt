package org.zstack.header.longjob;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by Camile on 2/5/18.
 */
public class LongJobMessageData implements LongJobData {
    protected final NeedReplyMessage needReplyMessage;

    public LongJobMessageData(NeedReplyMessage msg){
        this.needReplyMessage = msg;
    }

    public NeedReplyMessage getNeedReplyMessage() {
        return needReplyMessage;
    }
}
