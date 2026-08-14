package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 * Created by GuoYi on 8/26/18.
 */
public class DownloadBitsFromKVMHostToPrimaryStorageReply extends MessageReply {
    private String format;

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
