package org.zstack.header.protocol

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.protocol.RouterAreaInventory

doc {

	title "创建路由区域资源的返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.protocol.APICreateVRouterOspfAreaEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.4"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.protocol.APICreateVRouterOspfAreaEvent.inventory"
		desc "null"
		type "RouterAreaInventory"
		since "3.4"
		clz RouterAreaInventory.class
	}
}
