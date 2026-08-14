package org.zstack.iam1.entity.accounts

import java.sql.Timestamp

doc {

	title "账户组清单"

	field {
		name "uuid"
		desc "账户组的UUID，唯一标示该资源"
		type "String"
		since "4.10.0"
	}
	field {
		name "name"
		desc "账户组名称"
		type "String"
		since "4.10.0"
	}
	field {
		name "description"
		desc "账户组的详细描述"
		type "String"
		since "4.10.0"
	}
	field {
		name "parentUuid"
		desc "父账户组 UUID, null 表示无父账户组, 自己是最上层"
		type "String"
		since "4.10.0"
	}
	field {
		name "rootGroupUuid"
		desc "最上层账户组的 UUID; 如果自己就是最上层, 则 rootGroupUuid 指向自己"
		type "String"
		since "4.10.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.10.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.10.0"
	}
}
