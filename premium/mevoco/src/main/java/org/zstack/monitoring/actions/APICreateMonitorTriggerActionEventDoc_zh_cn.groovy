package org.zstack.monitoring.actions

import org.zstack.header.errorcode.ErrorCode

doc {

	title "创建报警器动作返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	ref {
		name "error"
		path "org.zstack.monitoring.actions.APICreateMonitorTriggerActionEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.monitoring.actions.APICreateMonitorTriggerActionEvent.inventory"
		desc "报警器动作清单"
		type "MonitorTriggerActionInventory"
		since "zsv_0.6"
		clz MonitorTriggerActionInventory.class
	}
}
