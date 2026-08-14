package org.zstack.header.storage.backup;

import org.zstack.header.message.OverlayMessage;
import org.zstack.header.volume.VolumeMessage;

public class VolumeBackupOverlayMsg extends OverlayMessage implements VolumeMessage {
    private String volumeUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
