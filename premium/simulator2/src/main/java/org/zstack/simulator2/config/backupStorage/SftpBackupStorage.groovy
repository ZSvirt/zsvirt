package org.zstack.simulator2.config.backupStorage

import org.zstack.simulator2.config.Col

/**
 * Created by xing5 on 2017/9/19.
 */
class SftpBackupStorage extends BackupStorage {
    @Col
    String ip
    @Col
    String path
}
