package org.zstack.zwatch.alarm.activealarm.api

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.alarm.activealarm.entity.ActiveAlarmInventory

doc {

	title "查询一键报警的报警器列表返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.10.0"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.alarm.activealarm.api.APIQueryActiveAlarmReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.10.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.zwatch.alarm.activealarm.api.APIQueryActiveAlarmReply.inventories"
		desc "一键报警的报警器清单列表"
		type "List"
		since "3.10.0"
		clz ActiveAlarmInventory.class
	}
}
