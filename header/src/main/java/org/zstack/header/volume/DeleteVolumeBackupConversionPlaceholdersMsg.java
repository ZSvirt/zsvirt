package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

import java.util.ArrayList;
import java.util.List;

public class DeleteVolumeBackupConversionPlaceholdersMsg extends NeedReplyMessage {
    private List<String> placeholderBackupUuids = new ArrayList<>();

    public List<String> getPlaceholderBackupUuids() {
        return placeholderBackupUuids;
    }

    public void setPlaceholderBackupUuids(List<String> placeholderBackupUuids) {
        this.placeholderBackupUuids = placeholderBackupUuids;
    }
}
