package org.zstack.log.server

import org.zstack.log.server.LogServerInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询日志服务器返回"

	ref {
		name "inventories"
		path "org.zstack.log.server.APIQueryLogServerReply.inventories"
		desc "null"
		type "List"
		since "1.0.0, zsv_5.1.0"
		clz LogServerInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "1.0.0, zsv_5.1.0"
	}
	ref {
		name "error"
		path "org.zstack.log.server.APIQueryLogServerReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "1.0.0, zsv_5.1.0"
		clz ErrorCode.class
	}
}
