package org.zstack.header.volume;

import org.zstack.header.message.ConfigurableTimeoutMessage;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.message.NeedReplyMessage;

import java.util.concurrent.TimeUnit;

@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 36)
public class ChangeVolumeEncryptionMsg extends NeedReplyMessage implements VolumeMessage, ConfigurableTimeoutMessage {
    private String volumeUuid;
    private boolean encrypted;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }
}
