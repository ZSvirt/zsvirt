package org.zstack.allocator.domain.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取调度域关联虚拟机的请求返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "1.0.1, zsv 5.2.0"
	}
	ref {
		name "error"
		path "org.zstack.allocator.domain.api.APIGetAllocatorDomainVmsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "1.0.1, zsv 5.2.0"
		clz ErrorCode.class
	}
	field {
		name "currentVms"
		desc "当前在域中的虚拟机UUID列表（SystemTag 当前值等于该域）"
		type "List"
		since "1.0.1, zsv 5.2.0"
	}
	field {
		name "nextStartVms"
		desc "下次启动会进入该域、且不在 currentVms 中的虚拟机UUID列表（ResourceConfig 配置值等于该域，当前值不是该域）"
		type "List"
		since "1.0.1, zsv 5.2.0"
	}
}
