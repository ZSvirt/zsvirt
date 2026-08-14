package org.zstack.header.identity.login

import org.zstack.header.identity.login.LoginAuthenticationProcedureDesc
import org.zstack.header.errorcode.ErrorCode

doc {

	title "登录的认证步骤的返回"

	ref {
		name "procedures"
		path "org.zstack.header.identity.login.APIGetLoginProceduresReply.procedures"
		desc "登录认证步骤信息列表"
		type "List"
		since "zsv 3.16.0"
		clz LoginAuthenticationProcedureDesc.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.16.0"
	}
	ref {
		name "error"
		path "org.zstack.header.identity.login.APIGetLoginProceduresReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.16.0"
		clz ErrorCode.class
	}
}
