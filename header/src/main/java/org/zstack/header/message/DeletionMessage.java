package org.zstack.header.message;

/**
 */
public class DeletionMessage extends NeedReplyMessage {
    private boolean isForceDelete;

    public boolean isForceDelete() {
        return isForceDelete;
    }

    public void setForceDelete(boolean isForceDelete) {
        this.isForceDelete = isForceDelete;
    }
}
