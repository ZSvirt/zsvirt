package org.zstack.guesttools

import org.zstack.header.errorcode.ErrorCode

doc {

	title "卸载虚拟机增强工具镜像"

	field {
		name "success"
		desc "卸载是否成功"
		type "boolean"
		since "4.10.16"
	}
	ref {
		name "error"
		path "org.zstack.guesttools.APIDetachGuestToolsIsoFromVmEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.10.16"
		clz ErrorCode.class
	}
}
