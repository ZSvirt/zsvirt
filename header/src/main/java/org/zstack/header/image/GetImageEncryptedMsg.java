package org.zstack.header.image;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @Author: DaoDao
 * @Date: 2021/11/5
 */
public class GetImageEncryptedMsg extends NeedReplyMessage implements ImageMessage {
    private String imageUuid;

    @Override
    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }
}
