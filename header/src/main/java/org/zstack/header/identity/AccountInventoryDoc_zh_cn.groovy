package org.zstack.header.identity

import java.sql.Timestamp

doc {

	title "账户清单"

	field {
		name "uuid"
		desc "账户的UUID，唯一标示该资源"
		type "String"
		since "zsv 4.0.0"
	}
	field {
		name "name"
		desc "账户名称"
		type "String"
		since "zsv 4.0.0"
	}
	field {
		name "description"
		desc "账户的详细描述"
		type "String"
		since "zsv 4.0.0"
	}
	field {
		name "type"
		desc "账户类型"
		type "String"
		since "zsv 4.0.0"
	}
	field {
		name "state"
		desc "账户状态"
		type "String"
		since "zsv 4.0.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 4.0.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 4.0.0"
	}
}
