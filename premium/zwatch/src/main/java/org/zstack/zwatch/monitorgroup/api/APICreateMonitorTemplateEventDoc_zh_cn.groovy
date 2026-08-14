package org.zstack.zwatch.monitorgroup.api

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.monitorgroup.entity.MonitorTemplateInventory

doc {

	title "创建监控模板返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.10.0"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.monitorgroup.api.APICreateMonitorTemplateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.10.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.zwatch.monitorgroup.api.APICreateMonitorTemplateEvent.inventory"
		desc "监控模板清单"
		type "MonitorTemplateInventory"
		since "3.10.0"
		clz MonitorTemplateInventory.class
	}
}
