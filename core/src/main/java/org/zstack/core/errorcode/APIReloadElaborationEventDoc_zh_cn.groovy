package org.zstack.core.errorcode

import org.zstack.header.errorcode.ErrorCode

doc {

	title "重新加载系统错误码文件结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.core.errorcode.APIReloadElaborationEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.3.0"
		clz ErrorCode.class
	}
}
