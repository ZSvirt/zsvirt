package org.zstack.billing.table

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "价目表详细信息"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.7"
	}
	field {
		name "name"
		desc "价目表名称"
		type "String"
		since "3.7"
	}
	field {
		name "description"
		desc "价目表描述"
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
