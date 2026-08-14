package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * Created by xing5 on 2016/7/20.
 */
public class GetVolumeRootImageUuidFromPrimaryStorageReply extends MessageReply {
    private String imageUuid;

    // maybe other snapshot tree based image uuids
    private List<String> otherImageUuids;

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public List<String> getOtherImageUuids() {
        return otherImageUuids;
    }

    public void setOtherImageUuids(List<String> otherImageUuids) {
        this.otherImageUuids = otherImageUuids;
    }
}
