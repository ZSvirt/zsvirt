package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * author:kaicai.hu
 * Date:2019/9/17
 */
public class UpdateSpiceChannelConfigMsg extends NeedReplyMessage implements HostMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getHostUuid() {
        return uuid;
    }
}
