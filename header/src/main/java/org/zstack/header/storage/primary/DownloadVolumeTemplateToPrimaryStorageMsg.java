package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;

public class DownloadVolumeTemplateToPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private ImageSpec templateSpec;
    private String hostUuid;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public ImageSpec getTemplateSpec() {
        return templateSpec;
    }

    public void setTemplateSpec(ImageSpec templateSpec) {
        this.templateSpec = templateSpec;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
