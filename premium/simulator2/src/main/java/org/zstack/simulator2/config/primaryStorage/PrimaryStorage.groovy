package org.zstack.simulator2.config.primaryStorage

import org.zstack.simulator2.config.Col
import org.zstack.simulator2.config.VO
import org.zstack.utils.SizeUtils

/**
 * Created by xing5 on 2017/9/15.
 */
abstract class PrimaryStorage extends VO {
    @Col
    Long totalCapacity = SizeUtils.sizeStringToBytes("10T")
    @Col
    Long availableCapacity = totalCapacity
}
