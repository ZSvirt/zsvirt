package org.zstack.vrouterRoute

import org.zstack.header.errorcode.ErrorCode
import org.zstack.vrouterRoute.VRouterRouteEntryInventory

doc {

	title "云路由路由条目清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	ref {
		name "error"
		path "org.zstack.vrouterRoute.APIQueryVRouterRouteEntryReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_2.1"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.vrouterRoute.APIQueryVRouterRouteEntryReply.inventories"
		desc "null"
		type "List"
		since "zsv_2.1"
		clz VRouterRouteEntryInventory.class
	}
}
