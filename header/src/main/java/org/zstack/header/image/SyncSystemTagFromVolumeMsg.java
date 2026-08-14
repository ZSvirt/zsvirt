package org.zstack.header.image;

import org.zstack.header.message.NeedReplyMessage;

public class SyncSystemTagFromVolumeMsg extends NeedReplyMessage implements ImageMessage {
    private String imageUuid;
    private String volumeUuid;

    @Override
    public String getImageUuid() {
        return imageUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }
}
