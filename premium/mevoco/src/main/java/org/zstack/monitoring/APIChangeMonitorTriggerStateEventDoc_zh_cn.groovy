package org.zstack.monitoring

import org.zstack.header.errorcode.ErrorCode

doc {

	title "修改报警器状态"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.monitoring.APIChangeMonitorTriggerStateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.1"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.monitoring.APIChangeMonitorTriggerStateEvent.inventory"
		desc "null"
		type "MonitorTriggerInventory"
		since "0.6"
		clz MonitorTriggerInventory.class
	}
}
