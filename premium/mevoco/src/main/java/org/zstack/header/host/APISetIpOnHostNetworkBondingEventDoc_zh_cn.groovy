package org.zstack.header.host

import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在 Bond 网口配置 IP 的请求返回"

	ref {
		name "inventory"
		path "org.zstack.header.host.APISetIpOnHostNetworkBondingEvent.inventory"
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
		path "org.zstack.header.host.APISetIpOnHostNetworkBondingEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.0"
		clz ErrorCode.class
	}
}
