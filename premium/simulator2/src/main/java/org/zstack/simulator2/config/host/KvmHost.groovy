package org.zstack.simulator2.config.host

import org.zstack.simulator2.config.Col

/**
 * Created by xing5 on 2017/9/16.
 */
class KvmHost extends Host {
    @Col
    String username
    @Col
    String password
}