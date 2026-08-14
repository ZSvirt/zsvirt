package org.zstack.header.vmscheduling

import org.zstack.header.errorcode.ErrorCode

doc {

	title "根据调度状态获取对应的虚拟机返回"

	field {
		name "uuids"
		desc "虚拟机的 UUID 列表"
		type "List"
		since "3.16.0"
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.16.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vmscheduling.APIListVmSchedulingRulesFromExecuteStateReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.16.0"
		clz ErrorCode.class
	}
}
