package org.zstack.storage.primary.sharedblock

doc {
    title "共享块逻辑卷所在LUN"

    category "sharedblock"

    field {
        name "wwid"
        desc "底层LUN的WWID"
        type "String"
        since "1.0.1, zsv_5.2.0"
    }
    field {
        name "capacityBytes"
        desc "从LBA 0起计算的完整原始LUN字节容量"
        type "Long"
        since "1.0.1, zsv_5.2.0"
    }
}
