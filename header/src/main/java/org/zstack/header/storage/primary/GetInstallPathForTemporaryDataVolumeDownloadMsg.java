package org.zstack.header.storage.primary;

/**
 * Created by MaJin on 2019/8/1.
 */
public class GetInstallPathForTemporaryDataVolumeDownloadMsg extends GetInstallPathForDataVolumeDownloadMsg {
    private String originVolumeUuid;

    public String getOriginVolumeUuid() {
        return originVolumeUuid;
    }

    public void setOriginVolumeUuid(String originVolumeUuid) {
        this.originVolumeUuid = originVolumeUuid;
    }

    public GetInstallPathForTemporaryDataVolumeDownloadMsg() {

    }

    public GetInstallPathForTemporaryDataVolumeDownloadMsg(String originVolumeUuid) {
        this.originVolumeUuid = originVolumeUuid;
    }
}
