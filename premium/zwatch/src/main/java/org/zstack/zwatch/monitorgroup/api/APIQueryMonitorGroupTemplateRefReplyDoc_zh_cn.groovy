package org.zstack.zwatch.monitorgroup.api

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupTemplateRefInventory

doc {

	title "查询资源分组应用的监控模板返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.10.0"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.monitorgroup.api.APIQueryMonitorGroupTemplateRefReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.10.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.zwatch.monitorgroup.api.APIQueryMonitorGroupTemplateRefReply.inventories"
		desc "资源分组应用的监控模板清单列表"
		type "List"
		since "3.10.0"
		clz MonitorGroupTemplateRefInventory.class
	}
}
