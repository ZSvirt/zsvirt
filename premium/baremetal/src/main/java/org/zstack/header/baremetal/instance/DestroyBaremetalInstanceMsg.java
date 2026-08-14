package org.zstack.header.baremetal.instance;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 7/8/18.
 */
public class DestroyBaremetalInstanceMsg extends NeedReplyMessage implements BaremetalInstanceMessage {
    private String uuid;
    private String deletionPolicy;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDeletionPolicy() {
        return deletionPolicy;
    }

    public void setDeletionPolicy(String deletionPolicy) {
        this.deletionPolicy = deletionPolicy;
    }

    @Override
    public String getBaremetalInstanceUuid() {
        return getUuid();
    }
}
