package org.zstack.header.protocol

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.protocol.NetworkRouterAreaRefInventory

doc {

	title "在这里输入结构的名称"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.protocol.APIQueryVRouterOspfNetworkReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.protocol.APIQueryVRouterOspfNetworkReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz NetworkRouterAreaRefInventory.class
	}
}
