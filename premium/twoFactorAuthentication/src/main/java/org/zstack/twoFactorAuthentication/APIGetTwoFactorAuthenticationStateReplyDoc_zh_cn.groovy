package org.zstack.twoFactorAuthentication

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取双因子认证状态的结果"

	ref {
		name "error"
		path "org.zstack.twoFactorAuthentication.APIGetTwoFactorAuthenticationStateReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.10.0"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.10.0"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "4.10.0"
	}
}
