package org.zstack.header.vm

import org.zstack.header.vm.CloneVmInstanceResults
import org.zstack.header.errorcode.ErrorCode

doc {

	title "从虚拟机模板创建虚拟机返回"

	ref {
		name "result"
		path "org.zstack.header.vm.APICreateVmInstanceFromTemplatedVmInstanceEvent.result"
		desc "null"
		type "CloneVmInstanceResults"
		since "zsv 4.2.6"
		clz CreateVmInstanceFromTemplatedVmInstanceResults.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.2.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APICreateVmInstanceFromTemplatedVmInstanceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.2.6"
		clz ErrorCode.class
	}
}
