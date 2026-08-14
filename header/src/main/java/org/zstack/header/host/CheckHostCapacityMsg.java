package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @ Author : yh.w
 * @ Date   : Created in 19:31 2020/6/23
 */
public class CheckHostCapacityMsg extends NeedReplyMessage implements HostMessage {

    private String hostUuid;

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }
}
