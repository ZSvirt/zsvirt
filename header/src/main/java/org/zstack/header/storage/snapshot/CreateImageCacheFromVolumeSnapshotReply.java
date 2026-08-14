package org.zstack.header.storage.snapshot;

import org.zstack.header.message.MessageReply;

/**
 * Created by MaJin on 2021/3/18.
 */
public class CreateImageCacheFromVolumeSnapshotReply extends MessageReply {
    private long actualSize;
    private String locationHostUuid;
    private String imageUrl;

    public long getActualSize() {
        return actualSize;
    }

    public void setActualSize(long actualSize) {
        this.actualSize = actualSize;
    }

    public String getLocationHostUuid() {
        return locationHostUuid;
    }

    public void setLocationHostUuid(String locationHostUuid) {
        this.locationHostUuid = locationHostUuid;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
