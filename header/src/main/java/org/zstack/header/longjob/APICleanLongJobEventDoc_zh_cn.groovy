package org.zstack.header.longjob

import org.zstack.header.errorcode.ErrorCode

doc {

	title "清理 Long Job 的结果"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.13.0"
	}
	ref {
		name "error"
		path "org.zstack.header.longjob.APICleanLongJobEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.13.0"
		clz ErrorCode.class
	}
}
