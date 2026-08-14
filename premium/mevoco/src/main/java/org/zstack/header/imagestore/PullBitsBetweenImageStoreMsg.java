package org.zstack.header.imagestore;

import org.zstack.header.message.NeedReplyMessage;

public class PullBitsBetweenImageStoreMsg extends NeedReplyMessage {
    private String srcImageStorageUuid;
    private String dstImageStorageUuid;
    private String installPath;

    public String getSrcImageStorageUuid() {
        return srcImageStorageUuid;
    }

    public void setSrcImageStorageUuid(String srcImageStorageUuid) {
        this.srcImageStorageUuid = srcImageStorageUuid;
    }

    public String getDstImageStorageUuid() {
        return dstImageStorageUuid;
    }

    public void setDstImageStorageUuid(String dstImageStorageUuid) {
        this.dstImageStorageUuid = dstImageStorageUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
