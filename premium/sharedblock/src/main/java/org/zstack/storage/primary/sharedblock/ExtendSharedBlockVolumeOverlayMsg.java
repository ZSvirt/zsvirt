package org.zstack.storage.primary.sharedblock;

import org.zstack.header.message.OverlayMessage;
import org.zstack.header.volume.VolumeMessage;

public class ExtendSharedBlockVolumeOverlayMsg extends OverlayMessage implements VolumeMessage {
    private String volumeUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
