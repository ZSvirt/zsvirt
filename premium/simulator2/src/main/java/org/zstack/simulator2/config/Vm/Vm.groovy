package org.zstack.simulator2.config.Vm

import org.zstack.simulator2.config.Col
import org.zstack.simulator2.config.VO
import org.zstack.simulator2.config.host.Host

/**
 * Created by xing5 on 2017/9/18.
 */
class Vm extends VO {
    @Col(parent = Host.class)
    String hostId
}
