package org.zstack.network.service.vip

import org.zstack.header.errorcode.ErrorCode

doc {

    title "虚拟IP清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
    ref {
        name "error"
        path "org.zstack.network.service.vip.APIUpdateVipEvent.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
        type "ErrorCode"
        since "0.6"
        clz ErrorCode.class
    }
    ref {
        name "inventory"
        path "org.zstack.network.service.vip.APIUpdateVipEvent.inventory"
        desc "虚拟IP清单"
        type "VipInventory"
        since "0.6"
        clz VipInventory.class
    }
}
