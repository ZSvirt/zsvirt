package org.zstack.header.host

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取云主机虚拟化技术类型"

	ref {
		name "error"
		path "org.zstack.header.host.APIGetHypervisorTypesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	field {
		name "hypervisorTypes"
		desc ""
		type "List"
		since "zsv 0.6"
	}
}
