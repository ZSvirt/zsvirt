package org.zstack.zsv.telemetry.header;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Notify peers to drop a pending report after successful Cloud upload.
 * Best-effort / idempotent: missing file is success.
 */
public class TelemetryDeletePendingMsg extends NeedReplyMessage {
    private String fileName;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
