package org.zstack.header.scheduler

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv_0.6"
	}
	field {
		name "targetResourceUuid"
		desc ""
		type "String"
		since "zsv_0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "zsv_0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "zsv_0.6"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "zsv_0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv_0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv_0.6"
	}
	field {
		name "triggersUuid"
		desc ""
		type "List"
		since "zsv_0.6"
	}
}
