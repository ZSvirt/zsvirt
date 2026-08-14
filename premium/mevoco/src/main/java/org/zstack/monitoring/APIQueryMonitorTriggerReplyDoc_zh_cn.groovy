package org.zstack.monitoring

import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询报警器返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.monitoring.APIQueryMonitorTriggerReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.monitoring.APIQueryMonitorTriggerReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz MonitorTriggerInventory.class
	}
}
