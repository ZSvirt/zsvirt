package org.zstack.header.vm;

import org.zstack.header.image.ImageMessage;
import org.zstack.header.message.OverlayMessage;

/**
 * Created by GuoYi on 11/4/17.
 */
public class ChangeVmImageOverlayMsg extends OverlayMessage implements ImageMessage {
    private String imageUuid;

    @Override
    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }
}
