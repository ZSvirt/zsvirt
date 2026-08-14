package org.zstack.header.storage.primary;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.message.NeedReplyMessage;

import java.util.concurrent.TimeUnit;

@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
public class DownloadBitsFromNbdToPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private String primaryStorageInstallPath;
    @NoLogging
    private String nbdExportUrl;
    private long bandWidth;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getPrimaryStorageInstallPath() {
        return primaryStorageInstallPath;
    }

    public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
        this.primaryStorageInstallPath = primaryStorageInstallPath;
    }

    public long getBandWidth() {
        return bandWidth;
    }

    public void setBandWidth(long bandWidth) {
        this.bandWidth = bandWidth;
    }

    public String getNbdExportUrl() {
        return nbdExportUrl;
    }

    public void setNbdExportUrl(String nbdExportUrl) {
        this.nbdExportUrl = nbdExportUrl;
    }

}
