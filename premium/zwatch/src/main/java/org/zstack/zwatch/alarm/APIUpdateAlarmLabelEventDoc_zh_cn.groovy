package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.AlarmLabelInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	ref {
		name "inventory"
		path "org.zstack.zwatch.alarm.APIUpdateAlarmLabelEvent.inventory"
		desc "null"
		type "AlarmLabelInventory"
		since "0.6"
		clz AlarmLabelInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.alarm.APIUpdateAlarmLabelEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
