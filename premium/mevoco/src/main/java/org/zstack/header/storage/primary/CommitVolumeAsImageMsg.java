package org.zstack.header.storage.primary;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 8/3/16.
 */
public class CommitVolumeAsImageMsg extends NeedReplyMessage implements PrimaryStorageMessage, CommitVolumeAsImageMessage {
    private String name;
    private String description;
    private String guestOsType;
    private String platform;
    private String architecture;
    private boolean system;
    private String volumeUuid;
    private String primaryStorageUuid;
    private List<String> backupStorageUuids = new ArrayList<>(); // TDOO fix it
    private SessionInventory session;
    private String resourceUuid;
    private String mediaType;
    private long volumeActualSize; // optional
    private boolean virtio;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    @Override
    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    @Override
    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    @Override
    public boolean isVirtio() {
        return virtio;

    }

    public void setVirtio(boolean virtio) {
        this.virtio = virtio;
    }

    @Override
    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    @Override
    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    @Override
    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    @Override
    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }

    @Override
    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public long getVolumeActualSize() {
        return volumeActualSize;
    }

    public void setVolumeActualSize(long volumeActualSize) {
        this.volumeActualSize = volumeActualSize;
    }
}
