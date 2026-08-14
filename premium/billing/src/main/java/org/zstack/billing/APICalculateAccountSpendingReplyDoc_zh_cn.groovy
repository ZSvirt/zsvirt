package org.zstack.billing

import org.zstack.billing.Spending
import org.zstack.header.errorcode.ErrorCode

doc {

    title "账户花费"

    ref {
        name "error"
        path "org.zstack.billing.APICalculateAccountSpendingReply.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
        type "ErrorCode"
        since "0.6"
        clz ErrorCode.class
    }
    field {
        name "total"
        desc "总额"
        type "double"
        since "0.6"
    }
    ref {
        name "spending"
        path "org.zstack.billing.APICalculateAccountSpendingReply.spending"
        desc "花费"
        type "List"
        since "0.6"
        clz Spending.class
    }
    field {
        name "success"
        desc "成功标志"
        type "boolean"
        since "0.6"
    }
}
