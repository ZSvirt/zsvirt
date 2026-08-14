package org.zstack.header.storage.primary;

/**
 * Created by lining on 2019/4/16.
 */
public class PrimaryStorageAllocateConfig {
    private String type;

    private String uuid;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String primaryStorageUuid) {
        this.uuid = primaryStorageUuid;
    }
}
