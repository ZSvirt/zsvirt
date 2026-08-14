package org.zstack.header.cbt

import org.zstack.header.cbt.CbtTaskStatus
import java.sql.Timestamp

doc {

	title "CBT任务清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "4.10.10"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "4.10.10"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "4.10.10"
	}
	ref {
		name "status"
		path "org.zstack.header.cbt.CbtTaskInventory.status"
		desc "null"
		type "CbtTaskStatus"
		since "4.10.10"
		clz CbtTaskStatus.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.10.10"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.10.10"
	}
}
