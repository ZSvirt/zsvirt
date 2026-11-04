package org.zstack.allocator.domain.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除调度域的请求返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "1.0.1, zsv_5.2.0"
	}
	ref {
		name "error"
		path "org.zstack.allocator.domain.api.APIDeleteAllocatorDomainEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "1.0.1, zsv_5.2.0"
		clz ErrorCode.class
	}
}
