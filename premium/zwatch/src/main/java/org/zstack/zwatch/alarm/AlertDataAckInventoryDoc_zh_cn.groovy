package org.zstack.zwatch.alarm

import java.lang.Long
import java.sql.Timestamp

doc {

	title "报警确认信息"

	field {
		name "alertDataUuid"
		desc "报警消息 UUID"
		type "String"
		since "3.10.0"
	}
	field {
		name "alertType"
		desc "报警类型"
		type "String"
		since "3.10.0"
	}
	field {
		name "ackPeriod"
		desc "沉默时间"
		type "Long"
		since "3.10.0"
	}
	field {
		name "resourceUuid"
		desc "报警目标资源 UUID"
		type "String"
		since "3.10.0"
	}
	field {
		name "ackDate"
		desc "确认时间"
		type "Timestamp"
		since "3.10.0"
	}
	field {
		name "resumeAlert"
		desc "恢复报警"
		type "boolean"
		since "3.10.0"
	}
	field {
		name "operatorAccountUuid"
		desc "确认账号"
		type "String"
		since "3.10.0"
	}
}
