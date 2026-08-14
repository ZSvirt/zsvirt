package org.zstack.header.identity

import org.zstack.header.errorcode.ErrorCode

doc {

	title "会话是否有效"

	ref {
		name "error"
		path "org.zstack.header.identity.APIValidateSessionReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	field {
		name "valid"
		desc "会话是否有效"
		type "boolean"
		since "zsv 0.6"
	}
}
