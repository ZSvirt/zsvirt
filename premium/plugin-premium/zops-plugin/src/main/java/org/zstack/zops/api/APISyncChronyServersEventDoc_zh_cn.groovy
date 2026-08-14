package org.zstack.zops.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "同步 Chrony 时间源的请求返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.21"
	}
	ref {
		name "error"
		path "org.zstack.zops.api.APISyncChronyServersEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.21"
		clz ErrorCode.class
	}
}
