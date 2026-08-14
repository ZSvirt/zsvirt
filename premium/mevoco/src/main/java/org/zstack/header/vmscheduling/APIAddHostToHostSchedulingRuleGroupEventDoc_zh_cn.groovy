package org.zstack.header.vmscheduling

import org.zstack.header.errorcode.ErrorCode

doc {

	title "添加主机到主机调度组的返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.16.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vmscheduling.APIAddHostToHostSchedulingRuleGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.16.0"
		clz ErrorCode.class
	}
}
