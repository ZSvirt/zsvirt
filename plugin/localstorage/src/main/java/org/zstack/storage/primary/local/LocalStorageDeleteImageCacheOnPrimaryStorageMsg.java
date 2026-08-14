package org.zstack.storage.primary.local;

import org.zstack.header.storage.primary.DeleteImageCacheOnPrimaryStorageMsg;

/**
 * Created by xing5 on 2016/7/22.
 */
public class LocalStorageDeleteImageCacheOnPrimaryStorageMsg extends DeleteImageCacheOnPrimaryStorageMsg {
    private String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
