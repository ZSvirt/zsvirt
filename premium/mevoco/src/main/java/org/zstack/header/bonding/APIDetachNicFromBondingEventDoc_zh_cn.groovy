package org.zstack.header.bonding

import org.zstack.header.errorcode.ErrorCode
import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory

doc {

	title "从 Bond 网口解绑 slave 结果"

	ref {
		name "inventory"
		path "org.zstack.header.bonding.APIDetachNicFromBondingEvent.inventory"
		desc "主机网络绑定清单"
		type "HostNetworkBondingInventory"
		since "3.17.11"
		clz HostNetworkBondingInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.11"
	}
	ref {
		name "error"
		path "org.zstack.header.bonding.APIDetachNicFromBondingEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.11"
		clz ErrorCode.class
	}
}
