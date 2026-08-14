package org.zstack.scheduler

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.scheduler.SchedulerJobHistoryInventory

doc {

	title "查询定时任务记录结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.scheduler.APIQuerySchedulerJobHistoryReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.5.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.scheduler.APIQuerySchedulerJobHistoryReply.inventories"
		desc "定时任务记录"
		type "List"
		since "3.5.0"
		clz SchedulerJobHistoryInventory.class
	}
}
