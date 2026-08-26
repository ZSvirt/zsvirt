package org.zstack.header.volume;

import org.zstack.header.message.MessageReply;

public class ConvertVolumeBackupEncryptionReply extends MessageReply {
    private VolumeBackupEncryptionConversionResult result;

    public VolumeBackupEncryptionConversionResult getResult() {
        return result;
    }

    public void setResult(VolumeBackupEncryptionConversionResult result) {
        this.result = result;
    }
}
