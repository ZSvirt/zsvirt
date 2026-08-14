package org.zstack.header.image;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:34 2023/12/14
 */
public class CalculateImageHashMsg extends NeedReplyMessage implements ImageMessage {

    private String uuid;
    private String backupStorageUuid;
    private String algorithm;

    public CalculateImageHashMsg(APICalculateImageHashMsg amsg) {
        uuid = amsg.getUuid();
        backupStorageUuid = amsg.getBackupStorageUuid();
        algorithm = amsg.getAlgorithm();
    }

    public CalculateImageHashMsg() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public String getImageUuid() {
        return uuid;
    }
}
