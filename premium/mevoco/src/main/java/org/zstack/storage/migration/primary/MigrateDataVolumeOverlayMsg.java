package org.zstack.storage.migration.primary;

import org.zstack.header.message.OverlayMessage;
import org.zstack.header.volume.VolumeMessage;

/**
 * Created by GuoYi on 9/19/17.
 */
public class MigrateDataVolumeOverlayMsg extends OverlayMessage implements VolumeMessage {
    private String volumeUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
