package org.zstack.softwarePackage.message;

import org.zstack.core.Platform;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.softwarePackage.header.APIUploadSoftwarePackageToBackupStorageMsg;

public class UploadSoftwarePackageToBackupStorageMsg extends NeedReplyMessage {
    private String resourceUuid;
    private String name;
    private String backupStorageUuid;
    @NoLogging(type = NoLogging.Type.Uri)
    private String url;
    private String installPath;
    private String type;
    private SessionInventory session;

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

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
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

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }

    public static UploadSoftwarePackageToBackupStorageMsg fromApiMessage(APIUploadSoftwarePackageToBackupStorageMsg apiMessage) {
        if (apiMessage == null) {
            throw new IllegalArgumentException("apiMessage cannot be null");
        }

        UploadSoftwarePackageToBackupStorageMsg message = new UploadSoftwarePackageToBackupStorageMsg();
        message.setResourceUuid(apiMessage.getResourceUuid() != null ? apiMessage.getResourceUuid() : Platform.getUuid());
        message.setName(apiMessage.getName());
        message.setBackupStorageUuid(apiMessage.getBackupStorageUuid());
        message.setUrl(apiMessage.getUrl());
        message.setInstallPath(apiMessage.getInstallPath());
        message.setType(apiMessage.getType());
        message.setSession(apiMessage.getSession());
        return message;
    }
}