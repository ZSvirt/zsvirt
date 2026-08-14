package org.zstack.header.identity.role.api

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.identity.role.RoleInventory

doc {

	title "更新角色结果"

	field {
		name "success"
		desc "更新角色是否成功"
		type "boolean"
		since "zsv 4.10.0"
	}
	ref {
		name "error"
		path "org.zstack.header.identity.role.api.APIUpdateRoleEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.10.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.identity.role.api.APIUpdateRoleEvent.inventory"
		desc "更新的角色信息, 附带角色权限条目"
		type "RoleInventory"
		since "zsv 4.10.0"
		clz RoleInventory.class
	}
}
