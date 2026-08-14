package org.zstack.network.l2.virtualSwitch.header

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取候选的主机Kernel适配器的返回结果"

	field {
		name "results"
		desc "候选的主机Kernel适配器的返回结果列表"
		type "List"
		since "4.10.20"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.10.20"
	}
	ref {
		name "error"
		path "org.zstack.network.l2.virtualSwitch.header.APIGetCandidateHostKernelInterfacesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.10.20"
		clz ErrorCode.class
	}
}
