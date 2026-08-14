package org.zstack.vrouterRoute

import org.zstack.header.errorcode.ErrorCode
import org.zstack.vrouterRoute.VRouterRouteEntryAO

doc {

	title "实时路由清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.vrouterRoute.APIGetVRouterRouteTableReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.1"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.vrouterRoute.APIGetVRouterRouteTableReply.inventories"
		desc "null"
		type "List"
		since "2.1"
		clz VRouterRouteEntryAO.class
	}
}
