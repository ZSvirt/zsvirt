package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 * Created by frank on 9/16/2015.
 */
public class DownloadImageToPrimaryStorageCacheReply extends MessageReply {
    private String installPath;

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
