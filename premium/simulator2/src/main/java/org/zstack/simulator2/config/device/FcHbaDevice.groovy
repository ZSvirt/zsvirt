package org.zstack.simulator2.config.device

import org.zstack.simulator2.config.Col

/**
 * @author shenjin
 * @date 2024/10/17
 */
class FcHbaDevice extends HbaDevice {
    @Col
    String portState
    @Col
    String speed
    @Col
    String supportedSpeeds
    @Col
    String symbolicName
    @Col
    String supportedClasses
    @Col
    String portName
    @Col
    String nodeName
}
