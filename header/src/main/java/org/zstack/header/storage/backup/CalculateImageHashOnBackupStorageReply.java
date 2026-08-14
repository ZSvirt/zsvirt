package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;
/**
 * @ Author : yh.w
 * @ Date   : Created in 17:53 2023/11/10
 */
public class CalculateImageHashOnBackupStorageReply extends MessageReply {
    private String hashValue;

    public String getHashValue() {
        return hashValue;
    }

    public void setHashValue(String hashValue) {
        this.hashValue = hashValue;
    }
}
