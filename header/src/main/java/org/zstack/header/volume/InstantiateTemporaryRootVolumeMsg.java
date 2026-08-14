package org.zstack.header.volume;

/**
 * Created by MaJin on 2019/2/13.
 */
public class InstantiateTemporaryRootVolumeMsg extends InstantiateRootVolumeMsg implements VolumeMessage {
    private String originVolumeUuid;

    public String getOriginVolumeUuid() {
        return originVolumeUuid;
    }

    public void setOriginVolumeUuid(String originVolumeUuid) {
        this.originVolumeUuid = originVolumeUuid;
    }
}
