package org.zstack.simulator2.config.primaryStorage

import org.zstack.simulator2.config.Col
import org.zstack.simulator2.config.VO

/**
 * Created by kayo on 2018/7/27.
 */
class BlockDevice extends VO {
    @Col
    String wwn
    @Col
    String hctl
    @Col
    String model
    @Col
    String vendor
    @Col
    String type
    @Col
    String serial
    @Col
    Long size
    @Col
    String wwids
    @Col(notNull = false)
    String multipathDeviceUuid
    @Col(notNull = false)
    String storageWwnn
    @Col(notNull = false)
    String hostIp
}
