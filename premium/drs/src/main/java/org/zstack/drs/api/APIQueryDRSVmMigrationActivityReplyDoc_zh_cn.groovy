package org.zstack.drs.api

import org.zstack.drs.entity.DRSVmMigrationActivityInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询集群 DRS 调度任务的结果"

	ref {
		name "inventories"
		path "org.zstack.drs.api.APIQueryDRSVmMigrationActivityReply.inventories"
		desc "集群 DRS 调度任务列表"
		type "List"
		since "4.0.0"
		clz DRSVmMigrationActivityInventory.class
	}
	field {
		name "success"
		desc "查询是否成功"
		type "boolean"
		since "4.0.0"
	}
	ref {
		name "error"
		path "org.zstack.drs.api.APIQueryDRSVmMigrationActivityReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.0.0"
		clz ErrorCode.class
	}
}
