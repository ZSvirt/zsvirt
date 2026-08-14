package org.zstack.simulator2.config.device

import org.zstack.simulator2.config.Col
import org.zstack.simulator2.config.VO

class MttyDevice extends VO {
    @Col
    String name
    @Col
    String uuid
    @Col
    String description
    @Col
    String hostIp
    @Col
    String type
    @Col
    String virtStatus
}
