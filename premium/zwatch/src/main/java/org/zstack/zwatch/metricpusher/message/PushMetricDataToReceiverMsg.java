package org.zstack.zwatch.metricpusher.message;

import org.zstack.header.message.NeedReplyMessage;

public class PushMetricDataToReceiverMsg extends NeedReplyMessage {
    private String receiverUuid;

    public String getReceiverUuid() {
        return receiverUuid;
    }

    public void setReceiverUuid(String receiverUuid) {
        this.receiverUuid = receiverUuid;
    }
}
