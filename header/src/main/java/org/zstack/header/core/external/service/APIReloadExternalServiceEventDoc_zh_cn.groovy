package org.zstack.header.core.external.service

import org.zstack.header.errorcode.ErrorCode

doc {

	title "重新加载External Services的返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.header.core.external.service.APIReloadExternalServiceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.17.0"
		clz ErrorCode.class
	}
}
