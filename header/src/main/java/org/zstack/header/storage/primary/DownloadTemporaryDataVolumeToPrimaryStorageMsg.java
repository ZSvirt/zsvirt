package org.zstack.header.storage.primary;

/**
 * Created by MaJin on 2019/8/1.
 */
public class DownloadTemporaryDataVolumeToPrimaryStorageMsg extends DownloadDataVolumeToPrimaryStorageMsg {
    private String originVolumeUuid;

    public String getOriginVolumeUuid() {
        return originVolumeUuid;
    }

    public void setOriginVolumeUuid(String originVolumeUuid) {
        this.originVolumeUuid = originVolumeUuid;
    }

    public DownloadTemporaryDataVolumeToPrimaryStorageMsg() {

    }

    public DownloadTemporaryDataVolumeToPrimaryStorageMsg(String originVolumeUuid) {
        this.originVolumeUuid = originVolumeUuid;
    }
}
