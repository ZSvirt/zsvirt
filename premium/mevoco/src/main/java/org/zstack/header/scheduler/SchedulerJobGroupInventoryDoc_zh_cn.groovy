package org.zstack.header.scheduler

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "定时任务组清单"

	field {
		name "uuid"
		desc "定时任务组的UUID，唯一标示该资源"
		type "String"
		since "3.4.0"
	}
	field {
		name "name"
		desc "定时任务组名称"
		type "String"
		since "3.4.0"
	}
	field {
		name "description"
		desc "定时任务组的详细描述"
		type "String"
		since "3.4.0"
	}
	field {
		name "state"
		desc "定时任务组的状态"
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
	field {
		name "jobData"
		desc "任务参数"
		type "String"
		since "3.4.0"
	}
	field {
		name "triggersUuid"
		desc "触发器UUID"
		type "List"
		since "3.4.0"
	}
}
