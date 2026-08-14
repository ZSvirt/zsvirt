package org.zstack.storage.ceph.primary;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

import java.io.Serializable;
import java.util.List;

/**
 * Created by GuoYi on 10/19/17.
 */
public class CephToCephMigrateVolumeSegmentMsg extends NeedReplyMessage implements PrimaryStorageMessage, Serializable {
    private String parentUuid;
    private String resourceUuid;
    private String srcInstallPath;
    private String dstInstallPath;
    private String primaryStorageUuid;
    private boolean isXsky;
    private String dstPrimaryStorageUuid;

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getSrcInstallPath() {
        return srcInstallPath;
    }

    public void setSrcInstallPath(String srcInstallPath) {
        this.srcInstallPath = srcInstallPath;
    }

    public String getDstInstallPath() {
        return dstInstallPath;
    }

    public void setDstInstallPath(String dstInstallPath) {
        this.dstInstallPath = dstInstallPath;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public boolean isXsky() {
        return isXsky;
    }

    public void setXsky(boolean xsky) {
        isXsky = xsky;
    }

    public String getDstPrimaryStorageUuid() {
        return dstPrimaryStorageUuid;
    }

    public void setDstPrimaryStorageUuid(String dstPrimaryStorageUuid) {
        this.dstPrimaryStorageUuid = dstPrimaryStorageUuid;
    }
}
