package org.zstack.softwarePackage.message;

import org.zstack.core.Platform;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.softwarePackage.header.APIUploadSoftwarePackageMsg;

public class UploadSoftwarePackageMsg extends NeedReplyMessage {
    private String resourceUuid;
    private String name;
    private String managementNodeUuid;
    private String hostUuid;
    @NoLogging(type = NoLogging.Type.Uri)
    private String url;
    private String installPath;
    private String type;

    public boolean needTrack() {
        return url != null && url.startsWith("upload://");
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public static UploadSoftwarePackageMsg fromApiMessage(APIUploadSoftwarePackageMsg apiMessage) {
        if (apiMessage == null) {
            throw new IllegalArgumentException("apiMessage cannot be null");
        }

        UploadSoftwarePackageMsg message = new UploadSoftwarePackageMsg();
        message.setResourceUuid(apiMessage.getResourceUuid() != null ? apiMessage.getResourceUuid() : Platform.getUuid());
        message.setName(apiMessage.getName());
        message.setManagementNodeUuid(apiMessage.getManagementNodeUuid());
        message.setHostUuid(apiMessage.getHostUuid());
        message.setUrl(apiMessage.getUrl());
        message.setInstallPath(apiMessage.getInstallPath());
        message.setType(apiMessage.getType());
        return message;
    }
}