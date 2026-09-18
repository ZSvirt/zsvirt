package org.zstack.billing

import org.zstack.billing.spendingcalculator.pcidevice.PricePciDeviceOfferingRefInventory

doc {

    title "价格清单"

    field {
        name "uuid"
        desc "资源的UUID，唯一标示该资源"
        type "String"
        since "zsv_0.6"
    }
    field {
        name "resourceName"
        desc "资源名称"
        type "String"
        since "zsv_0.6"
    }
    field {
        name "resourceUnit"
        desc "资源计费单元"
        type "String"
        since "zsv_0.6"
    }
    field {
        name "timeUnit"
        desc "计费时间单元"
        type "String"
        since "zsv_0.6"
    }
    field {
        name "price"
        desc "价格"
        type "Double"
        since "zsv_0.6"
    }
    field {
        name "dateInLong"
        desc "以长整型记录的时刻"
        type "Long"
        since "zsv_0.6"
    }
    field {
        name "createDate"
        desc "创建时间"
        type "Timestamp"
        since "zsv_0.6"
    }
    field {
        name "lastOpDate"
        desc "最后一次修改时间"
        type "Timestamp"
        since "zsv_0.6"
    }
    ref {
        name "pciDeviceOfferings"
        path "org.zstack.billing.PriceInventory.pciDeviceOfferings"
        desc "null"
        type "List"
        since "zsv_2.4"
        clz PricePciDeviceOfferingRefInventory.class
    }
    field {
        name "tableUuid"
        desc "价目表UUID"
        type "String"
        since "zsv_3.7"
    }
}
