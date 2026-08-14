package org.zstack.vrouterRoute

import org.zstack.header.errorcode.ErrorCode
import org.zstack.vrouterRoute.VirtualRouterVRouterRouteTableRefInventory

doc {

	title "云路由设备与云路由路由表绑定关系清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.vrouterRoute.APIQueryVirtualRouterVRouterRouteTableRefReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.1"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.vrouterRoute.APIQueryVirtualRouterVRouterRouteTableRefReply.inventories"
		desc "null"
		type "List"
		since "2.1"
		clz VirtualRouterVRouterRouteTableRefInventory.class
	}
}
