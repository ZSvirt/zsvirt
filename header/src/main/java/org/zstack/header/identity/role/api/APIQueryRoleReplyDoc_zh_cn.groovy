package org.zstack.header.identity.role.api

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.identity.role.RoleInventory

doc {

	title "查询角色结果"

	field {
		name "success"
		desc "查询是否成功"
		type "boolean"
		since "zsv 4.10.0"
	}
	ref {
		name "error"
		path "org.zstack.header.identity.role.api.APIQueryRoleReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.10.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.identity.role.api.APIQueryRoleReply.inventories"
		desc "角色列表"
		type "List"
		since "zsv 4.10.0"
		clz RoleInventory.class
	}
}
