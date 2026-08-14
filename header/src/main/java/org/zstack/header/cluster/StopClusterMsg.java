package org.zstack.header.cluster;

import org.zstack.header.message.NeedReplyMessage;

public class StopClusterMsg extends NeedReplyMessage {
    public StopClusterMsg() {
        super.timeout = 60 * 1000L;
    }
}
