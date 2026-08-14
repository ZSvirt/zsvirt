package org.zstack.simulator2.config.device

import org.zstack.simulator2.config.Col
import org.zstack.simulator2.config.VO

/**
 * Created by hyz on 2020/12/15.
 */
class MdevDeviceSpec extends VO{
    @Col
    String name
    @Col
    String vendor
    @Col
    String deviceID
    @Col
    String FBMemory
    @Col
    String maxInstances
    @Col
    String subSystemID
    @Col
    String displayHeads
    @Col
    String maxResolutionX
    @Col
    String maxResolutionY
    @Col
    String frameRateLimit
    @Col
    String GRIDLicense
}
