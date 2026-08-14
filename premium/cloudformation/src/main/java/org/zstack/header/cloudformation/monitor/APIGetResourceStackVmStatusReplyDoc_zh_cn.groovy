package org.zstack.header.cloudformation.monitor

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取资源栈内云主机的监控状态结果"

	ref {
		name "error"
		path "org.zstack.header.cloudformation.monitor.APIGetResourceStackVmStatusReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.9.0"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	field {
		name "portStatus"
		desc "端口监控状态(open / initialize / close)"
		type "Map"
		since "3.9.0"
	}
}
