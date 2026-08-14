package org.zstack.vrouterRoute

import org.zstack.header.errorcode.ErrorCode
import org.zstack.vrouterRoute.VRouterRouteTableInventory

doc {

	title "操作返回结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.vrouterRoute.APIUpdateVRouterRouteTableEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3.1"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.vrouterRoute.APIUpdateVRouterRouteTableEvent.inventory"
		desc "null"
		type "VRouterRouteTableInventory"
		since "2.3.1"
		clz VRouterRouteTableInventory.class
	}
}
