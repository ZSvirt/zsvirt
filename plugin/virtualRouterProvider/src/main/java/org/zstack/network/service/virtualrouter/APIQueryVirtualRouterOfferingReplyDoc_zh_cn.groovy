package org.zstack.network.service.virtualrouter

import org.zstack.header.errorcode.ErrorCode
import org.zstack.network.service.virtualrouter.VirtualRouterOfferingInventory

doc {

	title "虚拟路由器规格清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.network.service.virtualrouter.APIQueryVirtualRouterOfferingReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.network.service.virtualrouter.APIQueryVirtualRouterOfferingReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz VirtualRouterOfferingInventory.class
	}
}
