package org.zstack.header.network.l2

import org.zstack.header.network.l2.L2NetworkInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新二层网络虚拟网络ID结果"

	ref {
		name "inventory"
		path "org.zstack.header.network.l2.APIUpdateL2NetworkVirtualNetworkIdEvent.inventory"
		desc "null"
		type "L2NetworkInventory"
		since "zsv 4.10.0"
		clz L2NetworkInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.10.0"
	}
	ref {
		name "error"
		path "org.zstack.header.network.l2.APIUpdateL2NetworkVirtualNetworkIdEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.10.0"
		clz ErrorCode.class
	}
}
