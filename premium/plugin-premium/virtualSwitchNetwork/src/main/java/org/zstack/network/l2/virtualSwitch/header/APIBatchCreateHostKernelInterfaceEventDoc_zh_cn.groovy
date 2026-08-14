package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.HostKernelInterfaceResult
import org.zstack.header.errorcode.ErrorCode

doc {

	title "批量创建Kernel适配器的返回结果"

	ref {
		name "results"
		path "org.zstack.network.l2.virtualSwitch.header.APIBatchCreateHostKernelInterfaceEvent.results"
		desc "null"
		type "List"
		since "4.10.20"
		clz HostKernelInterfaceResult.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.10.20"
	}
	ref {
		name "error"
		path "org.zstack.network.l2.virtualSwitch.header.APIBatchCreateHostKernelInterfaceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.10.20"
		clz ErrorCode.class
	}
}
