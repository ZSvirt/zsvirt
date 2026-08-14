package org.zstack.header.volume;

import org.zstack.header.message.MessageReply;

/**
 * Created by MaJin on 2020/9/14.
 */
public class CreateImageCacheFromVolumeReply extends MessageReply {
    private String locateHostUuid;

    public String getLocateHostUuid() {
        return locateHostUuid;
    }

    public void setLocateHostUuid(String locateHostUuid) {
        this.locateHostUuid = locateHostUuid;
    }
}
