package org.zstack.billing

import org.zstack.billing.Spending
import org.zstack.header.errorcode.ErrorCode

doc {

    title "资源类型总花费"

    ref {
        name "error"
        path "org.zstack.billing.APICalculateResourceSpendingReply.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
        type "ErrorCode"
        since "zsv_3.4.0"
        clz ErrorCode.class
    }
    field {
        name "total"
        desc "总额"
        type "double"
        since "zsv_3.4.0"
    }
    ref {
        name "spending"
        path "org.zstack.billing.APICalculateResourceSpendingReply.spending"
        desc "花费"
        type "List"
        since "zsv_3.4.0"
        clz Spending.class
    }
    field {
        name "success"
        desc "成功标志"
        type "boolean"
        since "zsv_3.4.0"
    }
}
