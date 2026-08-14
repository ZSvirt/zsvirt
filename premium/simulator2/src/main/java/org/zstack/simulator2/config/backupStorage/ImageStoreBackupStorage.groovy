package org.zstack.simulator2.config.backupStorage

import org.zstack.simulator2.config.Col

/**
 * Created by xing5 on 2017/9/27.
 */
class ImageStoreBackupStorage extends BackupStorage {
    @Col
    String ip
    @Col
    String path
}
