package org.zstack.scheduler

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.scheduler.SchedulerJobGroupSchedulerTriggerRefInventory

doc {

	title "定时任务组绑定触发器结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.scheduler.APIAddSchedulerJobGroupToSchedulerTriggerEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.4.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.scheduler.APIAddSchedulerJobGroupToSchedulerTriggerEvent.inventory"
		desc "触发器定时任务组引用清单"
		type "SchedulerJobGroupSchedulerTriggerRefInventory"
		since "3.4.0"
		clz SchedulerJobGroupSchedulerTriggerRefInventory.class
	}
}
