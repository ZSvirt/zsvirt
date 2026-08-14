package org.zstack.network.service.virtualrouter

import org.zstack.header.errorcode.ErrorCode
import org.zstack.appliancevm.ApplianceVmInventory

doc {

	title "重连虚拟路由器返回值"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.network.service.virtualrouter.APIReconnectVirtualRouterEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.network.service.virtualrouter.APIReconnectVirtualRouterEvent.inventory"
		desc "null"
		type "ApplianceVmInventory"
		since "0.6"
		clz ApplianceVmInventory.class
	}
}
