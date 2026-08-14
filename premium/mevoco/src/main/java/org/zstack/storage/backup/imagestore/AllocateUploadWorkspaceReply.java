package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.MessageReply;

public class AllocateUploadWorkspaceReply extends MessageReply {
    private String uploadWorkspace;
    private String bsInstallPath;

    public String getUploadWorkspace() {
        return uploadWorkspace;
    }

    public void setUploadWorkspace(String uploadWorkspace) {
        this.uploadWorkspace = uploadWorkspace;
    }

    public String getBsInstallPath() {
        return bsInstallPath;
    }

    public void setBsInstallPath(String bsInstallPath) {
        this.bsInstallPath = bsInstallPath;
    }
}
