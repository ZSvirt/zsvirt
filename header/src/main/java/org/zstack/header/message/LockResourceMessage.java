package org.zstack.header.message;

/**
 */
public abstract class LockResourceMessage extends NeedReplyMessage {
    public static final String UNLOCK_CANONICAL_EVENT_PATH = "/resource/unlock";

    private String unlockKey;
    private String reason;

    public String getUnlockKey() {
        return unlockKey;
    }

    public void setUnlockKey(String unlockKey) {
        this.unlockKey = unlockKey;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
