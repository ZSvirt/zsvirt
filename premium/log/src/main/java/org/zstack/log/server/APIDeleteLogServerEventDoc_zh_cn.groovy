package org.zstack.log.server

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除日志服务器返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "1.0.0, zsv_5.1.0"
	}
	ref {
		name "error"
		path "org.zstack.log.server.APIDeleteLogServerEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "1.0.0, zsv_5.1.0"
		clz ErrorCode.class
	}
}
