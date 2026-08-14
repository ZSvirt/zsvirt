package org.zstack.simulator2.config.device

import org.zstack.simulator2.config.Col
import org.zstack.simulator2.config.VO

/**
 * @author shenjin
 * @date 2024/10/17
 */
abstract class HbaDevice extends VO {
    @Col
    String name
    @Col
    String hostIp
    @Col
    String hbaType
}
