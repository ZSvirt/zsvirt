package org.zstack.sso.header

import org.zstack.header.errorcode.ErrorCode
import org.zstack.sso.header.OAuth2TokenInventory

doc {

	title "获取 OAuth2 Token 返回"

	field {
		name "success"
		desc "获取是否成功"
		type "boolean"
		since "4.3.0"
	}
	ref {
		name "error"
		path "org.zstack.sso.header.APIGetOAuth2TokenReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.3.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.sso.header.APIGetOAuth2TokenReply.inventory"
		desc "OAuth2 Token 清单列表"
		type "OAuth2TokenInventory"
		since "4.3.0"
		clz OAuth2TokenInventory.class
	}
}
