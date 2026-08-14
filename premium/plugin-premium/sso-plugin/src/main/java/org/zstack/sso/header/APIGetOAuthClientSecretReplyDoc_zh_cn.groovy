package org.zstack.sso.header

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取 OAuth2 Client Secret 返回"

	field {
		name "success"
		desc "获取是否成功"
		type "boolean"
		since "5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.sso.header.APIGetOAuthClientSecretReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
	field {
		name "clientSecret"
		desc "OAuth2 客户端的 Client Secret"
		type "String"
		since "5.0.0"
	}
}
