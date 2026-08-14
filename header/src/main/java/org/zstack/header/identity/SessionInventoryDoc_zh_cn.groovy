package org.zstack.header.identity

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "accountUuid"
		desc "账户UUID"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "userUuid"
		desc "用户UUID"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "expiredDate"
		desc ""
		type "Timestamp"
		since "zsv 0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 0.6"
	}
}
