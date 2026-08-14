package org.zstack.header.identity.role

import java.sql.Timestamp

doc {

	title "角色清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "name"
		desc "角色名称"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "description"
		desc "角色的详细描述"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "type"
		desc "角色类型, 系统预定义的为 Predefined, 自定义的为 Customized"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 4.10.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 4.10.0"
	}
	ref {
		name "policies"
		path "org.zstack.header.identity.role.RoleInventory.policies"
		desc "角色权限条目, 字符串列表格式"
		type "List"
		since "zsv 4.10.0"
		clz String.class
	}
}
