package org.zstack.header.imagestore;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by mingjian.deng on 2017/9/12.
 */
public class RecoveryImageBetweenImageStoreMsg extends NeedReplyMessage {
    private String imageUuid;
    private String srcImageStorageUuid;
    private String dstImageStorageUuid;
    private String newImageUuid;

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

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

    public String getNewImageUuid() {
        return newImageUuid;
    }

    public void setNewImageUuid(String newImageUuid) {
        this.newImageUuid = newImageUuid;
    }
}
