package org.zstack.ovf.message;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by Qi Le on 2022/4/29
 */
public class DeleteOvaPackageMsg extends NeedReplyMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
