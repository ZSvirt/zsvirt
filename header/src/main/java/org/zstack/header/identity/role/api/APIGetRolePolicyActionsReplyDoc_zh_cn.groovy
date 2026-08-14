package org.zstack.header.identity.role.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取角色权限行动结果"

	field {
		name "inventories"
		desc "所有匹配的行动"
		type "List"
		since "zsv 4.10.0"
	}
	field {
		name "success"
		desc "获取调用是否成功"
		type "boolean"
		since "zsv 4.10.0"
	}
	ref {
		name "error"
		path "org.zstack.header.identity.role.api.APIGetActionsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.10.0"
		clz ErrorCode.class
	}
}
