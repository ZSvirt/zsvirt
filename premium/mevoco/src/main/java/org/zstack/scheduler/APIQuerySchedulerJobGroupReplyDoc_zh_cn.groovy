package org.zstack.scheduler

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.scheduler.SchedulerJobGroupInventory

doc {

	title "定时任务组查询结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	ref {
		name "error"
		path "org.zstack.scheduler.APIQuerySchedulerJobGroupReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_3.4.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.scheduler.APIQuerySchedulerJobGroupReply.inventories"
		desc "定时任务组清单"
		type "List"
		since "zsv_3.4.0"
		clz SchedulerJobGroupInventory.class
	}
}
