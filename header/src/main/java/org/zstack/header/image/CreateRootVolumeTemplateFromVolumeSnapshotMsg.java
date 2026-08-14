package org.zstack.header.image;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by MaJin on 2021/3/16.
 */
public class CreateRootVolumeTemplateFromVolumeSnapshotMsg extends NeedReplyMessage
        implements CreateRootVolumeTemplateMessage, CreateTemplateFromSnapshotMessage {
    private String resourceUuid;
    private String snapshotUuid;
    private String name;
    private String description;
    private List<String> backupStorageUuids;
    private String guestOsType;
    private String platform;
    private String architecture;
    private boolean system;
    private boolean virtio = true;
    private SessionInventory session;

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
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

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    @Override
    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean isVirtio() {
        return virtio;
    }

    public void setVirtio(boolean virtio) {
        this.virtio = virtio;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    @Override
    public String getGuestOsType() {
        return guestOsType;
    }

    @Override
    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    @Override
    public String getPlatform() {
        return platform;
    }

    @Override
    public void setPlatform(String platform) {
        this.platform = platform;
    }

    @Override
    public String getArchitecture() {
        return architecture;
    }

    @Override
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
}
