package org.zstack.storage.primary.sharedblock

doc {
    title "共享块逻辑卷物理区间"

    category "sharedblock"

    field {
        name "wwid"
        desc "源原始LUN的WWID"
        type "String"
        since "1.0.1, zsv_5.2.0"
    }
    field {
        name "lvOffsetBytes"
        desc "该区间在逻辑卷中的起始字节偏移"
        type "Long"
        since "1.0.1, zsv_5.2.0"
    }
    field {
        name "lunOffsetBytes"
        desc "该区间相对原始LUN LBA 0的起始字节偏移"
        type "Long"
        since "1.0.1, zsv_5.2.0"
    }
    field {
        name "lengthBytes"
        desc "区间字节长度"
        type "Long"
        since "1.0.1, zsv_5.2.0"
    }
}
