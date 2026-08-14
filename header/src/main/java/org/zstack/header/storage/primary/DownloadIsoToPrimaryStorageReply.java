package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 * Created by frank on 5/23/2015.
 */
public class DownloadIsoToPrimaryStorageReply extends MessageReply {
    private String installPath;

    private String protocol;

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
