package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

public class ValidateVolumeBackupEncryptionConversionMsg extends NeedReplyMessage {
    private VolumeInventory volume;
    private boolean targetEncrypted;

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }

    public boolean isTargetEncrypted() {
        return targetEncrypted;
    }

    public void setTargetEncrypted(boolean targetEncrypted) {
        this.targetEncrypted = targetEncrypted;
    }
}
