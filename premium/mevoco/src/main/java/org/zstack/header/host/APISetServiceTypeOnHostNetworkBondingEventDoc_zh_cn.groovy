package org.zstack.header.host

import org.zstack.header.host.HostNetworkBondingServiceRefInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在 Bond 网口配置网络服务类型的请求返回"

	ref {
		name "inventory"
		path "org.zstack.header.host.APISetServiceTypeOnHostNetworkBondingEvent.inventory"
		desc "主机与网络绑定服务映射清单"
		type "List"
		since "3.17.11"
		clz HostNetworkBondingServiceRefInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.11"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APISetServiceTypeOnHostNetworkBondingEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.11"
		clz ErrorCode.class
	}
}
