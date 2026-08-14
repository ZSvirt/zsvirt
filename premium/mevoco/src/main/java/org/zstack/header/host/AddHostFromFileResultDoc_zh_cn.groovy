package org.zstack.header.host

import org.zstack.header.errorcode.ErrorCode

doc {

	title "添加物理机结果"

	field {
		name "ip"
		desc "物理机IP"
		type "String"
		since "3.1.0"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "3.1.0"
	}
	ref {
		name "error"
		path "org.zstack.header.host.AddHostFromFileResult.error"
		desc "错误码。若成功则为null"
		type "ErrorCode"
		since "3.1.0"
		clz ErrorCode.class
	}
}
