package org.zstack.header.image;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by camile on 2/3/2018.
 * copy by APIAddImageMsg
 */
public class AddImageMsg extends NeedReplyMessage {
    private String name;
    private String description;
    @NoLogging(type = NoLogging.Type.Uri)
    private String url;
    private String mediaType;
    private String guestOsType;
    private boolean virtio;
    private boolean system;
    private String format;
    private String platform;
    private List<String> backupStorageUuids;
    private String type;
    private String architecture;
    private SessionInventory session;
    private String resourceUuid;

    public boolean needTrack() {
        return url != null && url.startsWith("upload://");
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public List<String> getBackupStorageUuids() {
        if (backupStorageUuids == null) {
            backupStorageUuids = new ArrayList<String>();
        }
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String type) {
        this.mediaType = type;
    }

    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    public boolean isVirtio() {
        return virtio;
    }

    public void setVirtio(boolean virtio) {
        this.virtio = virtio;
    }

    public String getType() {
        return type;
    }

    public void setType(String imageType) {
        this.type = imageType;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
}
