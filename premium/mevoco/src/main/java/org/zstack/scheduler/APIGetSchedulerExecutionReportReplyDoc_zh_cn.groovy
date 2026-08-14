package org.zstack.scheduler

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取定时任务执行报告结果"

	ref {
		name "error"
		path "org.zstack.scheduler.APIGetSchedulerExecutionReportReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.9.0"
		clz ErrorCode.class
	}
	field {
		name "successRecords"
		desc "成功记录"
		type "List"
		since "3.9.0"
	}
	field {
		name "failureRecords"
		desc "失败记录"
		type "List"
		since "3.9.0"
	}
	field {
		name "partialSuccessRecords"
		desc "部分成功记录"
		type "List"
		since "3.9.0"
	}
	field {
		name "waitingRecords"
		desc "等待执行完成记录"
		type "List"
		since "3.9.0"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
}
