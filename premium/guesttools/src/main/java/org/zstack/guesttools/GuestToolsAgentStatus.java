package org.zstack.guesttools;


import javax.annotation.Nullable;

public enum GuestToolsAgentStatus {
    RUNNING("Running"),
    NOT_RUNNING("NotRunning"),
    NOT_CONNECTED("Not Connected"),
    NOT_SUPPORTED("Not Supported");

    public final String status;

    GuestToolsAgentStatus(String status) {
        this.status = status;
    }

    @Nullable
    public static GuestToolsAgentStatus of(String status) {
        for (GuestToolsAgentStatus value : values()) {
            if (value.status.equals(status)) {
                return value;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return status;
    }
}
