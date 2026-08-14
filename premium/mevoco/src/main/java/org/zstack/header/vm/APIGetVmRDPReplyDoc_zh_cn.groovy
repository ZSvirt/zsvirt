package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询VM RDP开关状态"

	ref {
		name "error"
		path "org.zstack.header.vm.APIGetVmUsbRedirectReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.1.2"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	field {
		name "enable"
		desc "vm RDP 开关是否开启"
		type "boolean"
		since "2.1.2"
	}
}
