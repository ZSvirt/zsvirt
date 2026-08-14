package org.zstack.sso.header

import org.zstack.sso.header.OAuth2ClientInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "创建 OAuth2 客户端清单"

	ref {
		name "inventory"
		path "org.zstack.sso.header.APICreateOAuthClientEvent.inventory"
		desc "OAuth2 客户端清单"
		type "OAuth2ClientInventory"
		since "4.3.0"
		clz OAuth2ClientInventory.class
	}
	field {
		name "success"
		desc "创建是否成功"
		type "boolean"
		since "4.3.0"
	}
	ref {
		name "error"
		path "org.zstack.sso.header.APICreateOAuthClientEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.3.0"
		clz ErrorCode.class
	}
}
