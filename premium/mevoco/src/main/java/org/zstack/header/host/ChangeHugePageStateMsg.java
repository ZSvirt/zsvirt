package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @ Author : yh.w
 * @ Date   : Created in 16:22 2019/4/29
 */
public class ChangeHugePageStateMsg extends NeedReplyMessage implements HostMessage {

    private String hostUuid;

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
