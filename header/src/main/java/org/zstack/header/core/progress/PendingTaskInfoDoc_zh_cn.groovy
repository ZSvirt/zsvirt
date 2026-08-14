package org.zstack.header.core.progress

import java.lang.Long

doc {

	title "等待运行任务的信息"

	field {
		name "name"
		desc "任务名称"
		type "String"
		since "zsv 3.6.0"
	}
	field {
		name "className"
		desc "任务类名"
		type "String"
		since "zsv 3.6.0"
	}
	field {
		name "index"
		desc "序号"
		type "int"
		since "zsv 3.6.0"
	}
	field {
		name "pendingTime"
		desc "已等待的时间"
		type "long"
		since "zsv 3.6.0"
	}
	field {
		name "executionTime"
		desc "已执行的时间"
		type "Long"
		since "zsv 3.6.0"
	}
	field {
		name "context"
		desc "任务详情"
		type "String"
		since "zsv 3.6.0"
	}
	field {
		name "apiId"
		desc "任务对应的API ID"
		type "String"
		since "zsv 3.6.0"
	}
	field {
		name "apiName"
		desc "任务对应的API名称"
		type "String"
		since "zsv 3.6.0"
	}
}
