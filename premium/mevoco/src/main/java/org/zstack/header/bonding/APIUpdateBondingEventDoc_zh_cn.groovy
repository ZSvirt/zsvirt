package org.zstack.header.bonding

import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新 Bond 网口的请求返回"

	ref {
		name "inventory"
		path "org.zstack.header.bonding.APIUpdateBondingEvent.inventory"
		desc "Bond 网口清单"
		type "HostNetworkBondingInventory"
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
		path "org.zstack.header.bonding.APIUpdateBondingEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.0"
		clz ErrorCode.class
	}
}
