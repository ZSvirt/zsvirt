package org.zstack.simulator2.config.backupStorage

import org.zstack.simulator2.config.Col
import org.zstack.simulator2.config.VO

/**
 * Created by xing5 on 2017/9/19.
 */
class CephBackupStorageMon extends VO {
    @Col(parent = CephBackupStorage.class)
    String cephId
    @Col
    String ip
    @Col(notNull = false)
    String monAddr
}
