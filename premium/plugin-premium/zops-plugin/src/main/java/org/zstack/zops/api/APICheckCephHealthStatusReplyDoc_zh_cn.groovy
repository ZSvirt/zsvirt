package org.zstack.zops.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "检查ceph健康状态的返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "4.10.10"
	}
	ref {
		name "error"
		path "org.zstack.zops.api.APICheckCephHealthStatusReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.10.10"
		clz ErrorCode.class
	}
}
