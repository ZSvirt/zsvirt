package org.zstack.simulator2.config.primaryStorage

import org.zstack.simulator2.config.Col
import org.zstack.simulator2.config.VO

/**
 * Created by xing5 on 2017/9/15.
 */
class CephPrimaryStorage extends PrimaryStorage {
    @Col
    String fsid
}
