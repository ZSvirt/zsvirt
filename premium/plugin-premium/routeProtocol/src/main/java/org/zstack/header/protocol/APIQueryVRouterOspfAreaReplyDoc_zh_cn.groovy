package org.zstack.header.protocol

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.protocol.RouterAreaInventory

doc {

	title "查询区域路由的返回信息"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.protocol.APIQueryVRouterOspfAreaReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.4"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.protocol.APIQueryVRouterOspfAreaReply.inventories"
		desc "null"
		type "List"
		since "3.4"
		clz RouterAreaInventory.class
	}
}
