package org.zstack.header.volume;

import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.message.NeedReplyMessage;

import java.util.concurrent.TimeUnit;

@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 36)
public class ConvertVolumeBackupEncryptionMsg extends NeedReplyMessage {
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
