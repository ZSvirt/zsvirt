package org.zstack.billing;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by lining on 2019/11/12.
 */
public class UpdateResourcePriceMsg extends NeedReplyMessage {
    private String uuid;

    private Long endDateInLong;

    private boolean setEndDateInLongBaseOnCurrentTime;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Long getEndDateInLong() {
        return endDateInLong;
    }

    public void setEndDateInLong(Long endDateInLong) {
        this.endDateInLong = endDateInLong;
    }

    public boolean isSetEndDateInLongBaseOnCurrentTime() {
        return setEndDateInLongBaseOnCurrentTime;
    }

    public void setSetEndDateInLongBaseOnCurrentTime(boolean setEndDateInLongBaseOnCurrentTime) {
        this.setEndDateInLongBaseOnCurrentTime = setEndDateInLongBaseOnCurrentTime;
    }
}
