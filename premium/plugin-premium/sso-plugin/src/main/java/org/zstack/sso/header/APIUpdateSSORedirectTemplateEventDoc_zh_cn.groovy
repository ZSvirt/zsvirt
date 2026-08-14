package org.zstack.sso.header

import org.zstack.sso.header.SSORedirectTemplateInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "修改跳转模版的清单的请求返回"

	ref {
		name "inventory"
		path "org.zstack.sso.header.APIUpdateSSORedirectTemplateEvent.inventory"
		desc "跳转模版的清单"
		type "SSORedirectTemplateInventory"
		since "4.3.0"
		clz SSORedirectTemplateInventory.class
	}
	field {
		name "success"
		desc "修改是否成功"
		type "boolean"
		since "4.3.0"
	}
	ref {
		name "error"
		path "org.zstack.sso.header.APIUpdateSSORedirectTemplateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.3.0"
		clz ErrorCode.class
	}
}
