package org.zstack.header.host;

import org.zstack.header.message.CancelMessage;

/**
 * Created by MaJin on 2019/7/23.
 */
public class CancelHostTaskMsg extends CancelMessage implements HostMessage {
    private String hostUuid;
    private Integer times;
    private Integer interval;

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public Integer getTimes() {
        return times;
    }

    public void setTimes(Integer times) {
        this.times = times;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
