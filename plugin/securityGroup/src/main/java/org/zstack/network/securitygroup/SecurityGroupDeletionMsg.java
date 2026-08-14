package org.zstack.network.securitygroup;

import org.zstack.header.message.DeletionMessage;

/**
 * Created by kayo on 2018/7/16.
 */
public class SecurityGroupDeletionMsg extends DeletionMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
