package org.zstack.simulator2.config.backupStorage

import org.zstack.simulator2.config.Col
import org.zstack.simulator2.config.VO
import org.zstack.utils.SizeUtils

/**
 * Created by xing5 on 2017/9/19.
 */
abstract class BackupStorage extends VO {
    @Col
    Long totalCapacity = SizeUtils.sizeStringToBytes("10T")
    @Col
    Long availableCapacity = totalCapacity
}
