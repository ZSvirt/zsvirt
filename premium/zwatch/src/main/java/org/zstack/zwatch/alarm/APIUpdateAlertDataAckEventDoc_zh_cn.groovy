package org.zstack.zwatch.alarm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.alarm.AlertDataAckInventory

doc {

	title "更新报警确认信息返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.alarm.APIUpdateAlertDataAckEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.10.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.zwatch.alarm.APIUpdateAlertDataAckEvent.inventory"
		desc "null"
		type "AlertDataAckInventory"
		since "3.10.0"
		clz AlertDataAckInventory.class
	}
}
