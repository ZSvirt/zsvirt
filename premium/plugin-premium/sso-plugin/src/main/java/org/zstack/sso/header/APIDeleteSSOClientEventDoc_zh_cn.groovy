package org.zstack.sso.header

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除认证客户端清单"

	field {
		name "success"
		desc "删除是否成功"
		type "boolean"
		since "4.3.0"
	}
	ref {
		name "error"
		path "org.zstack.sso.header.APIDeleteSSOClientEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.3.0"
		clz ErrorCode.class
	}
}
