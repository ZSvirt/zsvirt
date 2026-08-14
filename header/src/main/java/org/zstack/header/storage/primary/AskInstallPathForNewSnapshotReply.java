package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 * Created by frank on 6/9/2015.
 */
public class AskInstallPathForNewSnapshotReply extends MessageReply {
    private String snapshotInstallPath;

    public String getSnapshotInstallPath() {
        return snapshotInstallPath;
    }

    public void setSnapshotInstallPath(String snapshotInstallPath) {
        this.snapshotInstallPath = snapshotInstallPath;
    }
}
