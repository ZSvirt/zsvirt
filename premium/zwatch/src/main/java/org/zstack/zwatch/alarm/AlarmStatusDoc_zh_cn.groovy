package org.zstack.zwatch.alarm



doc {

	title "报警器状态"

	field {
		name "OK"
		desc "监控中"
		type "AlarmStatus"
		since "2.3"
	}
	field {
		name "Alarm"
		desc "已触发"
		type "AlarmStatus"
		since "2.3"
	}
	field {
		name "InsufficientData"
		desc "数据不足"
		type "AlarmStatus"
		since "2.3"
	}
}
