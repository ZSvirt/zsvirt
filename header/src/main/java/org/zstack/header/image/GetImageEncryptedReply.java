package org.zstack.header.image;

import org.zstack.header.message.MessageReply;

/**
 * @Author: DaoDao
 * @Date: 2021/11/5
 */
public class GetImageEncryptedReply extends MessageReply {
    private String encrypt;
    private String imageUuid;

    public String getEncrypt() {
        return encrypt;
    }

    public void setEncrypt(String encrypt) {
        this.encrypt = encrypt;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }
}
