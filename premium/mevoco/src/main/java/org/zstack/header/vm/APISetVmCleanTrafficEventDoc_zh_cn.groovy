package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "改变虚拟机防IP欺骗启用状态"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APISetVmCleanTrafficEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_3.1.0"
		clz ErrorCode.class
	}
}
