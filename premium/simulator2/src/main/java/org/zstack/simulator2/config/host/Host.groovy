package org.zstack.simulator2.config.host

import org.zstack.simulator2.config.Col
import org.zstack.simulator2.config.VO
import org.zstack.utils.SizeUtils
import org.zstack.utils.data.SizeUnit

/**
 * Created by xing5 on 2017/9/16.
 */
abstract class Host extends VO {
    @Col
    String ip
    @Col
    Integer cpuNum = 8
    @Col
    Integer totalCpu = 80
    @Col
    Integer cpuSockets = 2
    @Col
    Integer cpuCores = 40
    @Col
    Integer usedCpu = 0
    @Col
    Long totalMemory = SizeUtils.sizeStringToBytes("32G")
    @Col
    Long usedMemory = 0

    Host() {
    }
}
