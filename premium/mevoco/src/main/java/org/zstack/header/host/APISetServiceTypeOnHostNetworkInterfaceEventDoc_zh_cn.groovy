package org.zstack.header.host

import org.zstack.header.host.HostNetworkInterfaceServiceRefInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在物理网口配置网络服务类型的请求返回"

	ref {
		name "inventory"
		path "org.zstack.header.host.APISetServiceTypeOnHostNetworkInterfaceEvent.inventory"
		desc "主机与网络接口服务映射清单"
		type "List"
		since "3.16.31"
		clz HostNetworkInterfaceServiceRefInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.11"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APISetServiceTypeOnHostNetworkInterfaceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.11"
		clz ErrorCode.class
	}
}
