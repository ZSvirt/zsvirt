package org.zstack.network.service.flat

import org.zstack.header.errorcode.ErrorCode
import org.zstack.network.service.flat.IpStatisticData
import java.lang.Long
import org.zstack.header.errorcode.ErrorCode

doc {

    title "获取三层网络IP使用情况统计返回值"

    ref {
        name "error"
        path "org.zstack.network.service.flat.APIGetL3NetworkIpStatisticReply.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
        type "ErrorCode"
        since "3.7.0"
        clz ErrorCode.class
    }
    ref {
        name "ipStatistics"
        path "org.zstack.network.service.flat.APIGetL3NetworkIpStatisticReply.ipStatistics"
        desc "IP使用情况统计结果列表"
        type "List"
        since "3.7.0"
        clz IpStatisticData.class
    }
    field {
        name "total"
        desc "IP统计结果总数"
        type "Long"
        since "3.7.0"
    }
    field {
        name "success"
        desc "成功"
        type "boolean"
        since "3.7.0"
    }
}
