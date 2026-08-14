package org.zstack.header.storage.snapshot;

import org.zstack.header.message.NeedQuotaCheckMessage;
import org.zstack.header.message.NeedReplyMessage;

public class CreateVolumeSnapshotMsg extends NeedReplyMessage implements NeedQuotaCheckMessage {
    private String accountUuid;
    private String resourceUuid;
    private String volumeUuid;
    private String name;
    private String description;
    private SnapshotMode requiredSnapshotMode = SnapshotMode.AUTO;

    public SnapshotMode getRequiredSnapshotMode() {
        return requiredSnapshotMode;
    }

    public void setRequiredSnapshotMode(SnapshotMode requiredSnapshotMode) {
        this.requiredSnapshotMode = requiredSnapshotMode;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
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
}
