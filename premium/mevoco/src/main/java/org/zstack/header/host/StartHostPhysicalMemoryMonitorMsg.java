package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/22 15:15
 */
public class StartHostPhysicalMemoryMonitorMsg extends NeedReplyMessage implements HostMessage {

    private String hostUuid;

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }
}
