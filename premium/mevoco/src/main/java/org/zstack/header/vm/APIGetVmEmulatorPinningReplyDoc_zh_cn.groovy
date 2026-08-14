package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取虚拟机 EmulatorPinning 绑定的主机 CPU 的请求返回"

	ref {
		name "error"
		path "org.zstack.header.vm.APIGetVmEmulatorPinningReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.13.12"
		clz ErrorCode.class
	}
	field {
		name "emulatorPinning"
		desc ""
		type "String"
		since "3.13.12"
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.13.12"
	}
}
