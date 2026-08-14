package org.zstack.header.storage.backup;

import org.zstack.header.core.Completion;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.Message;

import java.util.List;

public interface BackupStorage {
    void handleMessage(Message msg);

    void deleteHook();

    void changeStateHook(BackupStorageStateEvent evt, BackupStorageState nextState);

    void attachHook(String zoneUuid, Completion completion);

    void detachHook(Completion completion);

    List<ImageInventory> scanImages();

    String idPrefix = "BS-";

    static String buildId(String uuid) {
        return idPrefix + uuid;
    }
}
