package org.zstack.header.scheduler

import java.sql.Timestamp

doc {

	title "定时任务记录"

	field {
		name "id"
		desc ""
		type "long"
		since "3.5.0"
	}
	field {
		name "triggerUuid"
		desc "定时器UUID"
		type "String"
		since "3.5.0"
	}
	field {
		name "schedulerJobUuid"
		desc "定时任务UUID"
		type "String"
		since "3.5.0"
	}
	field {
		name "schedulerJobGroupUuid"
		desc "定时任务组UUID"
		type "String"
		since "3.5.0"
	}
	field {
		name "startTime"
		desc "开始时间"
		type "Timestamp"
		since "3.5.0"
	}
	field {
		name "executeTime"
		desc "执行时长"
		type "long"
		since "3.5.0"
	}
	field {
		name "targetResourceUuid"
		desc "目标资源UUID"
		type "String"
		since "3.5.0"
	}
	field {
		name "requestDump"
		desc "任务请求"
		type "String"
		since "3.5.0"
	}
	field {
		name "resultDump"
		desc "任务结果"
		type "String"
		since "3.5.0"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "3.5.0"
	}
}
