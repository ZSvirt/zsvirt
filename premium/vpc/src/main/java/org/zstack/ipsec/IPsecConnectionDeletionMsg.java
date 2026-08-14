package org.zstack.ipsec;

import org.zstack.header.message.DeletionMessage;

/**
 * Created by MaJin on 2017-04-20.
 */
public class IPsecConnectionDeletionMsg extends DeletionMessage implements IPsecConnectionMessage {
    private String uuid;

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public String getIPsecConnectionUuid() {
        return uuid;
    }
}
