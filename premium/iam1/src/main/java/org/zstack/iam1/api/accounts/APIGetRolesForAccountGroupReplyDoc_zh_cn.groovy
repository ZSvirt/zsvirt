package org.zstack.iam1.api.accounts

import org.zstack.header.identity.role.RoleInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取账户组绑定的角色结果"

	ref {
		name "inventories"
		path "org.zstack.iam1.api.accounts.APIGetRolesForAccountGroupReply.inventories"
		desc "账户组绑定的角色"
		type "List"
		since "4.10.0"
		clz RoleInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.10.0"
	}
	ref {
		name "error"
		path "org.zstack.iam1.api.accounts.APIGetRolesForAccountGroupReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.10.0"
		clz ErrorCode.class
	}
}
