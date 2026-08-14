package org.zstack.header.vmscheduling

import org.zstack.header.errorcode.ErrorCode

doc {

	title "根据调度规则以及状态获取虚拟机的请求返回"

	field {
		name "uuids"
		desc "虚拟机 UUID 列表"
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
		path "org.zstack.header.vmscheduling.APIListVmsFromSchedulingStateReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.16.0"
		clz ErrorCode.class
	}
}
