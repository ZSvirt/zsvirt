package org.zstack.billing.table

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "账号价目表关联关系"

	field {
		name "accountUuid"
		desc "账户UUID"
		type "String"
		since "3.7"
	}
	field {
		name "tableUuid"
		desc "价目表UUID"
		type "String"
		since "3.7"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.7"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.7"
	}
}
