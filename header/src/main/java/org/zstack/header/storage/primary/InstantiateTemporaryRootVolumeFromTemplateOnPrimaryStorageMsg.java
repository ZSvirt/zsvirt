package org.zstack.header.storage.primary;

/**
 * Created by MaJin on 2019/2/13.
 */
public class InstantiateTemporaryRootVolumeFromTemplateOnPrimaryStorageMsg
        extends InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg implements PrimaryStorageMessage {
    private String originVolumeUuid;

    public String getOriginVolumeUuid() {
        return originVolumeUuid;
    }

    public void setOriginVolumeUuid(String originVolumeUuid) {
        this.originVolumeUuid = originVolumeUuid;
    }
}
