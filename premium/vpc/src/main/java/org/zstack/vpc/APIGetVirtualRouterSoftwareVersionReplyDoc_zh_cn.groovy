package org.zstack.vpc

import org.zstack.network.service.virtualrouter.VirtualRouterSoftwareVersionInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取VPC软件版本"

	ref {
		name "inventories"
		path "org.zstack.vpc.APIGetVirtualRouterSoftwareVersionReply.inventories"
		desc "null"
		type "List"
		since "4.5"
		clz VirtualRouterSoftwareVersionInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.5"
	}
	ref {
		name "error"
		path "org.zstack.vpc.APIGetVirtualRouterSoftwareVersionReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.5"
		clz ErrorCode.class
	}
}
