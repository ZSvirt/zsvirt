package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @Author : jingwang
 * @create 2023/4/20 6:15 PM
 */
public class PowerOnHostMsg extends NeedReplyMessage implements HostMessage {
    private String uuid;
    private boolean returnEarly;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isReturnEarly() {
        return returnEarly;
    }

    public void setReturnEarly(boolean returnEarly) {
        this.returnEarly = returnEarly;
    }

    @Override
    public String getHostUuid() {
        return uuid;
    }
}
