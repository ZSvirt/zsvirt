package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "更改云主机优先级返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIUpdateVmPriorityEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 3.7"
		clz ErrorCode.class
	}
}
