package org.zstack.header.bonding

import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "创建 Bond 网口结果"

	ref {
		name "inventory"
		path "org.zstack.header.bonding.APICreateBondingEvent.inventory"
		desc "Bond 网口清单列表"
		type "List"
		since "3.17.0"
		clz HostNetworkBondingInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.header.bonding.APICreateBondingEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.0"
		clz ErrorCode.class
	}
}
