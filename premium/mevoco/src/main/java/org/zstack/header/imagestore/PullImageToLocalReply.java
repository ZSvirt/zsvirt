package org.zstack.header.imagestore;

import org.zstack.header.message.MessageReply;

public class PullImageToLocalReply extends MessageReply{
    private String localInstallPath;

    public String getLocalInstallPath() {
        return localInstallPath;
    }

    public void setLocalInstallPath(String localInstallPath) {
        this.localInstallPath = localInstallPath;
    }
}
