package org.zstack.loginControl.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "解锁登录次数限制返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "3.5.1"
	}
	ref {
		name "error"
		path "org.zstack.loginControl.api.APIUnlockIdentityReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.5.1"
		clz ErrorCode.class
	}
}
