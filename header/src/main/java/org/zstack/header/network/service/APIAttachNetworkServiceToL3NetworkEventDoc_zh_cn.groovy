package org.zstack.header.network.service

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.network.l3.L3NetworkInventory

doc {

	title "三层网络清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.network.service.APIAttachNetworkServiceToL3NetworkEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.network.service.APIAttachNetworkServiceToL3NetworkEvent.inventory"
		desc "null"
		type "L3NetworkInventory"
		since "zsv 0.6"
		clz L3NetworkInventory.class
	}
}
