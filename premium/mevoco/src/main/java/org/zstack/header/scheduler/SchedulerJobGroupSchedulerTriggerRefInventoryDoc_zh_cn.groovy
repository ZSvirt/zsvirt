package org.zstack.header.scheduler

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "定时任务组与触发器引用清单"

	field {
		name "schedulerJobGroupUuid"
		desc "定时任务组UUID"
		type "String"
		since "3.4.0"
	}
	field {
		name "schedulerTriggerUuid"
		desc "触发器UUID"
		type "String"
		since "3.4.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.4.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.4.0"
	}
}
