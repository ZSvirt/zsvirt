package org.zstack.header.host

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取 CPU 分配信息的请求返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.13.12"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIGetHostResourceAllocationEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.13.12"
		clz ErrorCode.class
	}
	field {
		name "name"
		desc "主机名称"
		type "String"
		since "3.13.12"
	}
	field {
		name "uuid"
		desc "主机 UUID"
		type "String"
		since "3.13.12"
	}
	field {
		name "vCPUPin"
		desc "CPU 分配信息"
		type "List"
		since "3.13.12"
	}
}
