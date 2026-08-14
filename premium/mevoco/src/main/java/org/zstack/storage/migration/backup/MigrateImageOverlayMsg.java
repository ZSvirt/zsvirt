package org.zstack.storage.migration.backup;

import org.zstack.header.image.ImageMessage;
import org.zstack.header.message.OverlayMessage;

/**
 * Created by GuoYi on 9/19/17.
 */
public class MigrateImageOverlayMsg extends OverlayMessage implements ImageMessage {
    private String imageUuid;

    @Override
    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }
}
