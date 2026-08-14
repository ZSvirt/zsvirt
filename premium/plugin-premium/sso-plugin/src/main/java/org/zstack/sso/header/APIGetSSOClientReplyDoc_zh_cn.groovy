package org.zstack.sso.header

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取 SSO 客户端返回"

	field {
		name "success"
		desc "获取是否成功"
		type "boolean"
		since "4.3.0"
	}
	ref {
		name "error"
		path "org.zstack.sso.header.APIGetSSOClientReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.3.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.sso.header.APIGetSSOClientReply.inventories"
		desc "SSO 客户端清单列表"
		type "List"
		since "4.3.0"
		clz Map.class
	}
}
