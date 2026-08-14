package org.zstack.storage.ceph.primary;

import org.zstack.header.message.MessageReply;

/**
 * Created by GuoYi on 2018-12-07.
 */
public class CreateEmptyVolumeReply extends MessageReply {
    private long size;
    private boolean isXsky;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isXsky() {
        return isXsky;
    }

    public void setXsky(boolean xsky) {
        isXsky = xsky;
    }
}