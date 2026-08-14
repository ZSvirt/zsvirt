package org.zstack.monitoring

import org.zstack.header.errorcode.ErrorCode

doc {

	title "创建报警器"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.monitoring.APICreateMonitorTriggerEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.monitoring.APICreateMonitorTriggerEvent.inventory"
		desc "null"
		type "MonitorTriggerInventory"
		since "2.1"
		clz MonitorTriggerInventory.class
	}
}
