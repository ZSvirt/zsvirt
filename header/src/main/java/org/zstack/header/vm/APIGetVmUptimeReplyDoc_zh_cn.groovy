package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取虚拟机开机时间结果"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.18.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIGetVmUptimeReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.18.0"
		clz ErrorCode.class
	}
	field {
		name "uptime"
		desc "虚拟机开机时间"
		type "String"
		since "zsv 3.18.0"
	}
}
