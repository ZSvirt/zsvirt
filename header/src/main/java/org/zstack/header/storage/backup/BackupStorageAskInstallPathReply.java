package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;

/**
 * Created by frank on 7/6/2015.
 */
public class BackupStorageAskInstallPathReply extends MessageReply {
    private String installPath;

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
