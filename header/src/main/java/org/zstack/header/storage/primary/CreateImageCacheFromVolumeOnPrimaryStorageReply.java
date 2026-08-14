package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 * Created by MaJin on 2020/9/14.
 */
public class CreateImageCacheFromVolumeOnPrimaryStorageReply extends MessageReply {
    private String locateHostUuid;
    private long actualSize;

    public String getLocateHostUuid() {
        return locateHostUuid;
    }

    public void setLocateHostUuid(String locateHostUuid) {
        this.locateHostUuid = locateHostUuid;
    }

    public long getActualSize() {
        return actualSize;
    }

    public void setActualSize(long actualSize) {
        this.actualSize = actualSize;
    }
}
