package org.zstack.zwatch.alarm.activealarm.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "修改一键报警状态返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.10.0"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.alarm.activealarm.api.APIChangeActiveAlarmStateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.10.0"
		clz ErrorCode.class
	}
}
