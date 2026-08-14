package org.zstack.header.vmscheduling

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除虚拟机调度组的请求返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.16.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vmscheduling.APIDeleteVmSchedulingRuleGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.16.0"
		clz ErrorCode.class
	}
}
