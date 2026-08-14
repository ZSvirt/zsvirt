package org.zstack.core.rest;

import org.zstack.header.message.Message;

public class DeleteRestApiVOMsg extends Message {
    private long retentionDay;

    public long getRetentionDay() {
        return retentionDay;
    }

    public void setRetentionDay(long retentionDay) {
        this.retentionDay = retentionDay;
    }
}
