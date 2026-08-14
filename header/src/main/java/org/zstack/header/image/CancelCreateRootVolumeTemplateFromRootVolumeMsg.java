package org.zstack.header.image;

import org.zstack.header.message.CancelMessage;

import java.util.List;

/**
 * Created by MaJin on 2019/7/23.
 */
public class CancelCreateRootVolumeTemplateFromRootVolumeMsg extends CancelMessage {
    private String imageUuid;
    private String rootVolumeUuid;

    public String getRootVolumeUuid() {
        return rootVolumeUuid;
    }

    public void setRootVolumeUuid(String rootVolumeUuid) {
        this.rootVolumeUuid = rootVolumeUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }
}
