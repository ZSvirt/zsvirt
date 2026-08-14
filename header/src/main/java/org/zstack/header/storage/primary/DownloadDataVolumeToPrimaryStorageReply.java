package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 */
public class DownloadDataVolumeToPrimaryStorageReply extends MessageReply {
    private String installPath;
    private String format;
    private String protocol;

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

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
