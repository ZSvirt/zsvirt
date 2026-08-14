package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;

/**
 * Created by mingjian.deng on 17/2/21.
 */
public class ExportImageFromBackupStorageReply extends MessageReply {
    private String imageLocalPath;
    private String imageUrl;
    private String md5sum;

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }

    public String getImageLocalPath() {
        return imageLocalPath;
    }

    public void setImageLocalPath(String imageLocalPath) {
        this.imageLocalPath = imageLocalPath;
    }
}

