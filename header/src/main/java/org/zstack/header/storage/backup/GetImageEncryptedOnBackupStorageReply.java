package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;

/**
 * @Author: DaoDao
 * @Date: 2021/11/5
 */
public class GetImageEncryptedOnBackupStorageReply extends MessageReply {
    private String encrypted;

    public String getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(String encrypted) {
        this.encrypted = encrypted;
    }
}
