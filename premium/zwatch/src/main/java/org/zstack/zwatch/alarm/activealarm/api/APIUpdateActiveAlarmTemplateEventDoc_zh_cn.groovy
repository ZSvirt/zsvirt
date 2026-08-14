package org.zstack.zwatch.alarm.activealarm.api

import org.zstack.zwatch.alarm.activealarm.entity.ActiveAlarmTemplateInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "修改一键报警模板返回"

	ref {
		name "inventory"
		path "org.zstack.zwatch.alarm.activealarm.api.APIUpdateActiveAlarmTemplateEvent.inventory"
		desc "报警模板清单"
		type "ActiveAlarmTemplateInventory"
		since "3.17.0"
		clz ActiveAlarmTemplateInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.alarm.activealarm.api.APIUpdateActiveAlarmTemplateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.0"
		clz ErrorCode.class
	}
}
