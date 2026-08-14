package org.zstack.core.debug

import org.zstack.header.errorcode.ErrorCode

doc {

	title "Available debug signals"

	ref {
		name "error"
		path "org.zstack.core.debug.APIGetDebugSignalReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	field {
		name "signals"
		desc ""
		type "List"
		since "3.6.0"
	}
}
