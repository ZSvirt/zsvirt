package org.zstack.zwatch.monitorgroup.api

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupAlarmInventory

doc {

	title "查询资源分组报警器列表返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.10.0"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.monitorgroup.api.APIQueryMonitorGroupAlarmReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.10.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.zwatch.monitorgroup.api.APIQueryMonitorGroupAlarmReply.inventories"
		desc "资源分组报警器清单列表"
		type "List"
		since "3.10.0"
		clz MonitorGroupAlarmInventory.class
	}
}
