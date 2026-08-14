package org.zstack.header.cloudformation.monitor

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除资源栈内云主机端口监控结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.cloudformation.monitor.APIDeleteResourceStackVmPortMonitorEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.9.0"
		clz ErrorCode.class
	}
}
