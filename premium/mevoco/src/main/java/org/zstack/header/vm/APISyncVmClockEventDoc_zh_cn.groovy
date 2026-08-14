package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "立即同步虚拟机时钟结果"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.14.12"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APISyncVmClockEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.14.12"
		clz ErrorCode.class
	}
}
