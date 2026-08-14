package org.zstack.externalbackup;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.message.OverlayMessage;
import org.zstack.header.volume.VolumeMessage;

/**
 * Created by MaJin on 2019/11/28.
 */
public class VolumeExternalBackupOverlayMsg extends OverlayMessage implements VolumeMessage {
    private String volumeUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
