package org.zstack.header.vm;

import org.zstack.header.message.OverlayMessage;
import org.zstack.header.volume.VolumeMessage;

/**
 * Created by MaJin on 2017-07-08.
 */
public class ReimageVolumeOverlayMsg extends OverlayMessage implements VolumeMessage {
    private String volumeUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
