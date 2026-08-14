package org.zstack.core.eventlog

import org.zstack.core.eventlog.EventLogInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询事件日志的结果"

	ref {
		name "inventories"
		path "org.zstack.core.eventlog.APIQueryEventLogReply.inventories"
		desc "事件日志清单列表"
		type "List"
		since "3.12.0"
		clz EventLogInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.12.0"
	}
	ref {
		name "error"
		path "org.zstack.core.eventlog.APIQueryEventLogReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.12.0"
		clz ErrorCode.class
	}
}
