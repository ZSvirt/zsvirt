package org.zstack.header.cluster;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by MaJin on 2019/7/4.
 */
public abstract class PowerOffHardwareMsg extends NeedReplyMessage {
    private boolean waitTaskCompleted;
    private Long maxWaitTime;
    abstract public boolean powerOffManagementNode();
    abstract public boolean powerOffOurself();
    abstract public List getInventories();
    abstract public List<String> getUuids();

    public boolean isWaitTaskCompleted() {
        return waitTaskCompleted;
    }

    public void setWaitTaskCompleted(boolean waitTaskCompleted) {
        this.waitTaskCompleted = waitTaskCompleted;
    }

    public Long getMaxWaitTime() {
        return maxWaitTime;
    }

    public void setMaxWaitTime(Long maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
    }
}
