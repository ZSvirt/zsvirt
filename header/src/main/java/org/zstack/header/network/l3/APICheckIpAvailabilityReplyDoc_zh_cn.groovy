package org.zstack.header.network.l3

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCode

doc {

	title "可用性结果"

	ref {
		name "error"
		path "org.zstack.header.network.l3.APICheckIpAvailabilityReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	field {
		name "available"
		desc "可用"
		type "boolean"
		since "zsv 0.6"
	}
	field {
		name "reason"
		desc "原因"
		type "String"
		since "zsv 3.4"
	}
	field {
		name "success"
		desc "成功"
		type "boolean"
		since "zsv 0.6"
	}
}
