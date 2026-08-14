package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by MaJin on 2019/7/2.
 */
public class ShutdownHostMsg extends NeedReplyMessage implements HostMessage {
    private String uuid;
    private boolean waitTaskCompleted;
    private Long maxWaitTime;
    private boolean returnEarly;
    private HostPowerManagementMethod method;
    private boolean force = false;

    @Override
    public String getHostUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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

    public boolean isReturnEarly() {
        return returnEarly;
    }

    public void setReturnEarly(boolean returnEarly) {
        this.returnEarly = returnEarly;
    }

    public HostPowerManagementMethod getMethod() {
        return method;
    }

    public void setMethod(HostPowerManagementMethod method) {
        this.method = method;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }
}
