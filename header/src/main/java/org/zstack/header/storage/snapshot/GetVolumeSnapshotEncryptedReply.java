package org.zstack.header.storage.snapshot;

import org.zstack.header.message.MessageReply;

/**
 * @Author: DaoDao
 * @Date: 2021/11/8
 */
public class GetVolumeSnapshotEncryptedReply extends MessageReply {
    private String snapshotUuid;
    private String encrypt;

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }

    public String getEncrypt() {
        return encrypt;
    }

    public void setEncrypt(String encrypt) {
        this.encrypt = encrypt;
    }
}
