package org.zstack.header.storage.snapshot;

import org.zstack.header.message.MulitpleOverlayMsg;
import org.zstack.header.volume.VolumeMessage;

/**
 * Created by david on 11/25/16.
 */
public class VolumeSnapshotOverlayMsg extends MulitpleOverlayMsg implements VolumeMessage {
    private String volumeUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
