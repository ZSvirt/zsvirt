package org.zstack.zwatch.alarm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.alarm.AlarmInventory

doc {

	title "添加报警动作返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.alarm.APIAddActionToAlarmEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.zwatch.alarm.APIAddActionToAlarmEvent.inventory"
		desc "null"
		type "AlarmInventory"
		since "2.3"
		clz AlarmInventory.class
	}
}
