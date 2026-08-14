package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 */
public class ReInitRootVolumeFromTemplateOnPrimaryStorageReply extends MessageReply {
    private String newVolumeInstallPath;

    public String getNewVolumeInstallPath() {
        return newVolumeInstallPath;
    }

    public void setNewVolumeInstallPath(String newVolumeInstallPath) {
        this.newVolumeInstallPath = newVolumeInstallPath;
    }
}
