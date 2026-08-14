package org.zstack.header.storage.backup;

import org.zstack.header.tag.SystemTagLifeCycleListener;
import org.zstack.storage.backup.imagestore.ImageStoreSystemTags;

/**
 * Created by mingjian.deng on 2020/12/8.
 */
public class BackupHelper {
    public static void installLifeCycleListener(SystemTagLifeCycleListener listener) {
        ImageStoreSystemTags.ALLOW_BACKUP.installLifeCycleListener(listener);
        ImageStoreSystemTags.IS_REMOTE_BACKUP.installLifeCycleListener(listener);
        ImageStoreSystemTags.ONLY_FOR_BACKUP.installLifeCycleListener(listener);
    }
}
