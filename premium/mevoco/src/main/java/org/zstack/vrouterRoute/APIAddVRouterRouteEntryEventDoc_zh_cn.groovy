package org.zstack.vrouterRoute

import org.zstack.header.errorcode.ErrorCode
import org.zstack.vrouterRoute.VRouterRouteEntryInventory

doc {

	title "路由条目清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.vrouterRoute.APIAddVRouterRouteEntryEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.1"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.vrouterRoute.APIAddVRouterRouteEntryEvent.inventory"
		desc "null"
		type "VRouterRouteEntryInventory"
		since "2.1"
		clz VRouterRouteEntryInventory.class
	}
}
