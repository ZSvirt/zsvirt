package org.zstack.zwatch.alarm.activealarm.entity

import java.sql.Timestamp

doc {

	title "一键报警报警器"

	field {
		name "templateUuid"
		desc "模板 UUID"
		type "String"
		since "3.10.0"
	}
	field {
		name "alarmUuid"
		desc "报警器 UUID"
		type "String"
		since "3.10.0"
	}
	field {
		name "namespace"
		desc "名字空间"
		type "String"
		since "3.10.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.10.0"
	}
	field {
		name "uuid"
		desc "资源的 UUID，唯一标示该资源"
		type "String"
		since "3.10.0"
	}
}
