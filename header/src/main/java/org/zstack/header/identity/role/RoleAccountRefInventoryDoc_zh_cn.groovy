package org.zstack.header.identity.role

import java.sql.Timestamp

doc {

	title "角色账户关系清单"

	field {
		name "roleUuid"
		desc "角色 UUID"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "accountUuid"
		desc "账户 UUID"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "accountPermissionFrom"
		desc "账户权限来源, 如果账户是通过加入某个账户组而获得的角色, 那么这个值就是那个账户组的 UUID。如果账户直接绑定了角色, 这个值为 null"
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
}
