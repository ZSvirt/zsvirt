package org.zstack.storage.ceph.primary;

import org.zstack.header.message.MessageReply;

/**
 * Created by xing5 on 2016/4/29.
 */
public class UploadBitsToBackupStorageReply extends MessageReply {
    private String installPath;

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
