package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import java.lang.Integer

doc {

	title "获取vm支持的屏幕个数"

	ref {
		name "error"
		path "org.zstack.header.vm.APIGetVmMonitorNumberReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	field {
		name "monitorNumber"
		desc ""
		type "Integer"
		since "0.6"
	}
}
