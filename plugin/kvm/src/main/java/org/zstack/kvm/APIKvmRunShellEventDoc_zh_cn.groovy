package org.zstack.kvm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.kvm.APIKvmRunShellEvent.ShellResult

doc {

	title "KVM执行shell结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.kvm.APIKvmRunShellEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.kvm.APIKvmRunShellEvent.inventory"
		desc "null"
		type "Map"
		since "0.6"
		clz ShellResult.class
	}
}
