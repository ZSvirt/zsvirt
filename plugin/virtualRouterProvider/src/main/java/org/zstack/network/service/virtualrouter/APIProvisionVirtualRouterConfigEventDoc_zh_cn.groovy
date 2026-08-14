package org.zstack.network.service.virtualrouter

import org.zstack.header.errorcode.ErrorCode
import org.zstack.appliancevm.ApplianceVmInventory

doc {

	title "重新配置虚拟路由器的结果"

	ref {
		name "error"
		path "org.zstack.network.service.virtualrouter.APIProvisionVirtualRouterConfigEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.10"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.network.service.virtualrouter.APIProvisionVirtualRouterConfigEvent.inventory"
		desc "null"
		type "ApplianceVmInventory"
		since "3.10"
		clz ApplianceVmInventory.class
	}
	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "0.6"
	}
}
