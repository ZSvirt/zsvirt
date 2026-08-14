package org.zstack.simulator2.config.baremetal2

import org.zstack.simulator2.config.Col
import org.zstack.simulator2.config.VO

class BareMetal2Chassis extends VO {
    @Col
    String name
    @Col
    String description
    @Col
    String ipmiAddress
    @Col
    Integer ipmiPort
    @Col
    String ipmiUsername
    @Col
    String ipmiPassword
}
