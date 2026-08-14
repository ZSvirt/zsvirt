package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "云主机控制台密码"

	ref {
		name "error"
		path "org.zstack.header.vm.APIGetVmConsolePasswordReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	field {
		name "password"
		desc "控制台密码"
		type "String"
		since "zsv 0.6"
	}
}
