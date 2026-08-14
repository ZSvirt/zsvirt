package org.zstack.zwatch.monitorgroup.api

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupInventory

doc {

	title "创建资源分组返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.10.0"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.monitorgroup.api.APICreateMonitorGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.10.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.zwatch.monitorgroup.api.APICreateMonitorGroupEvent.inventory"
		desc "资源分组清单"
		type "MonitorGroupInventory"
		since "3.10.0"
		clz MonitorGroupInventory.class
	}
}
