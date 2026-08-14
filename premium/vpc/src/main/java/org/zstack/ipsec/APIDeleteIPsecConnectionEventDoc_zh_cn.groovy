package org.zstack.ipsec

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除IPSec连接返回值"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.ipsec.APIDeleteIPsecConnectionEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
