package org.zstack.billing

doc {

    title "花费"

    field {
        name "spendingType"
        desc "花费类型"
        type "String"
        since "zsv_0.6"
    }
    field {
        name "spending"
        desc "花费总额"
        type "double"
        since "zsv_0.6"
    }
    field {
        name "dateStart"
        desc "计费起始日期"
        type "Long"
        since "zsv_0.6"
    }
    field {
        name "dateEnd"
        desc "计费结束日期"
        type "Long"
        since "zsv_0.6"
    }
    ref {
        name "details"
        path "org.zstack.billing.Spending.details"
        desc "花费详情列表"
        type "List"
        since "zsv_0.6"
        clz SpendingDetails.class
    }
}
