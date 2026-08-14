package org.zstack.kvm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	field {
		name "returnCode"
		desc ""
		type "int"
		since "0.6"
	}
	field {
		name "stdout"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "stderr"
		desc ""
		type "String"
		since "0.6"
	}
	ref {
		name "errorCode"
		path "org.zstack.kvm.APIKvmRunShellEvent.ShellResult.errorCode"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
