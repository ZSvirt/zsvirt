package org.zstack.iam1.api.accounts

import org.zstack.header.identity.AccountInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取账户组下所有账户和组的结果"

	ref {
		name "inventories"
		path "org.zstack.iam1.api.accounts.APIGetAccountsInAccountGroupReply.inventories"
		desc "账户组下所有账户"
		type "List"
		since "4.10.0"
		clz AccountInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.10.0"
	}
	ref {
		name "error"
		path "org.zstack.iam1.api.accounts.APIGetAccountsInAccountGroupReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.10.0"
		clz ErrorCode.class
	}
}
