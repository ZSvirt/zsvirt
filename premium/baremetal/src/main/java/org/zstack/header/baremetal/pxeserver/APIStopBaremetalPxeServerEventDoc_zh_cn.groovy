package org.zstack.header.baremetal.pxeserver

import org.zstack.header.errorcode.ErrorCode

doc {

	title "停止PXE服务返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "APIStopBaremetalPxeServerEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.6.0"
		clz ErrorCode.class
	}
}
