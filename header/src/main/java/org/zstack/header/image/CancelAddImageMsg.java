package org.zstack.header.image;

import org.zstack.header.message.CancelMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by MaJin on 2019/7/11.
 */
public class CancelAddImageMsg extends CancelMessage implements ImageMessage {
    private AddImageMsg msg;
    private String imageUuid;

    public AddImageMsg getMsg() {
        return msg;
    }

    public void setMsg(AddImageMsg msg) {
        this.msg = msg;
    }

    @Override
    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }
}
