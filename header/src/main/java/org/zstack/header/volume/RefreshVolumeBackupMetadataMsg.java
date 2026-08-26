package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

import java.util.ArrayList;
import java.util.List;

public class RefreshVolumeBackupMetadataMsg extends NeedReplyMessage {
    private List<String> backupUuids = new ArrayList<>();

    public List<String> getBackupUuids() {
        return backupUuids;
    }

    public void setBackupUuids(List<String> backupUuids) {
        this.backupUuids = backupUuids;
    }
}
