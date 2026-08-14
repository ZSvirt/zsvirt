package org.zstack.simulator2.config.primaryStorage

import org.zstack.simulator2.config.Col
import org.zstack.simulator2.config.VO

/**
 * Created by kayo on 2018/8/2.
 */
class AgentProperty extends VO {
    @Col
    String name
    @Col
    String value
}
