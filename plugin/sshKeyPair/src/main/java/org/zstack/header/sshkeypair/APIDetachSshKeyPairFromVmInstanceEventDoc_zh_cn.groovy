package org.zstack.header.sshkeypair

import org.zstack.header.errorcode.ErrorCode

doc {

	title "虚拟机卸载密钥对返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.21"
	}
	ref {
		name "error"
		path "org.zstack.header.sshkeypair.APIDetachSshKeyPairFromVmInstanceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.21"
		clz ErrorCode.class
	}
}
