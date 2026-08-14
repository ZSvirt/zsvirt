package org.zstack.storage.primary.local;

import org.zstack.header.message.OverlayMessage;
import org.zstack.header.volume.VolumeMessage;

/**
 * Created by xing5 on 2016/7/21.
 */
public class MigrateVolumeOverlayMsg extends OverlayMessage implements VolumeMessage {
    private String volumeUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
