package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

public class CreateTemplateFromRootVolumeVmReply extends MessageReply {
    private String installPath;
    private String format;

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
}
