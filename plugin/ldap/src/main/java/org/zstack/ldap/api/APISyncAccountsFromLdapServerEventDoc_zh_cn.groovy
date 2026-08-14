package org.zstack.ldap.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "从 LDAP 服务器上同步用户结果"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.3.0"
	}
	ref {
		name "error"
		path "org.zstack.ldap.api.APISyncAccountsFromLdapServerEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.3.0"
		clz ErrorCode.class
	}
}
