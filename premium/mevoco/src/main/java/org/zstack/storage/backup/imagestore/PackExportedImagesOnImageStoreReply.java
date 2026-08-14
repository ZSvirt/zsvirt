package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.MessageReply;

/**
 * Created by Qi Le on 2022/4/29
 *
 * Pack some exported images on image store bs.
 * NOTICE: It is the caller's responsibility for updating the exportUrl of the package.
 */
public class PackExportedImagesOnImageStoreReply extends MessageReply {
    private String md5Sum;
    private String exportUrl;
    private Long size;

    public String getMd5Sum() {
        return md5Sum;
    }

    public void setMd5Sum(String md5Sum) {
        this.md5Sum = md5Sum;
    }

    public String getExportUrl() {
        return exportUrl;
    }

    public void setExportUrl(String exportUrl) {
        this.exportUrl = exportUrl;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
