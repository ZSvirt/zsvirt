package org.zstack.header.image;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by MaJin on 2021/3/16.
 */
public class CreateDataVolumeTemplateFromVolumeSnapshotMsg extends NeedReplyMessage
        implements CreateDataVolumeTemplateMessage, CreateTemplateFromSnapshotMessage {
    private String resourceUuid;
    private String snapshotUuid;
    private String name;
    private String description;
    private List<String> backupStorageUuids;
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

    public void setDescription(String description) {
        this.description = description;
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
}
