package org.zstack.simulator2.config.primaryStorage

import org.zstack.simulator2.config.Col

/**
 * Created by xing5 on 2017/9/15.
 */
class NfsPrimaryStorage extends PrimaryStorage {
    @Col
    String ip
    @Col
    String mountPoint
}
