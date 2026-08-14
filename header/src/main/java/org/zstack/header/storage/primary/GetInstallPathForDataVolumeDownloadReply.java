package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 * Created by mingjian.deng on 2018/1/12.
 */
public class GetInstallPathForDataVolumeDownloadReply extends MessageReply {
    private String installPath;

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
