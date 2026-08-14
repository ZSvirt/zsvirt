package org.zstack.header.volume;

import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by MaJin on 2020/9/14.
 */
public class CreateImageCacheFromVolumeMsg extends NeedReplyMessage implements VolumeMessage {
    private String uuid;
    private ImageInventory image;

    @Override
    public String getVolumeUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public ImageInventory getImage() {
        return image;
    }

    public void setImage(ImageInventory image) {
        this.image = image;
    }
}
