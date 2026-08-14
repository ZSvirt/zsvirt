package org.zstack.header.host

import org.zstack.header.errorcode.ErrorCode

doc {

	title "分配的物理机计算资源信息"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.11"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIAllocateHostResourceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.11"
		clz ErrorCode.class
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.17.11"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.17.11"
	}
	field {
		name "vCPUPin"
		desc "为对应vCPU数量分配的pCPU信息"
		type "List"
		since "3.17.11"
	}
}
