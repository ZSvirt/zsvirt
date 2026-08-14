package org.zstack.drs.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "执行DRS调度的结果"

	field {
		name "success"
		desc "执行是否成功"
		type "boolean"
		since "4.0.0"
	}
	ref {
		name "error"
		path "org.zstack.drs.api.APIExecuteDRSSchedulingEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.0.0"
		clz ErrorCode.class
	}
}
