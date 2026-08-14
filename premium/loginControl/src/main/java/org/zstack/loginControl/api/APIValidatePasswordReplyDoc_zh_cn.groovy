package org.zstack.loginControl.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "校验用户身份返回"

	field {
		name "available"
		desc "是否允许操作"
		type "boolean"
		since "3.10.0"
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.10.0"
	}
	ref {
		name "error"
		path "org.zstack.loginControl.api.APIValidatePasswordReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.10.0"
		clz ErrorCode.class
	}
}
