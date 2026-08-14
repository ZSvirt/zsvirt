package org.zstack.header.image;

import org.zstack.header.message.CancelMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by MaJin on 2019/7/23.
 */
public class CancelCreateDataVolumeTemplateFromVolumeMsg extends CancelMessage {
    private String imageUuid;
    private String volumeUuid;

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
