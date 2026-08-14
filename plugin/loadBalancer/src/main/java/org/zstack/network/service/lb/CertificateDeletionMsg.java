package org.zstack.network.service.lb;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by kayo on 2018/7/16.
 */
public class CertificateDeletionMsg extends NeedReplyMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
