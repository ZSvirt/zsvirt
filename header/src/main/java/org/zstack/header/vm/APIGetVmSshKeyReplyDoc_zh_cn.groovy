package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "云主机SSH Key"

	ref {
		name "error"
		path "org.zstack.header.vm.APIGetVmSshKeyReply.error"
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
		name "sshKey"
		desc "云主机SSH Key"
		type "String"
		since "zsv 0.6"
	}
}
