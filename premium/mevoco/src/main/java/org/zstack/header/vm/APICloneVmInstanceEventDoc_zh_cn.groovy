package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.CloneVmInstanceResults
import org.zstack.header.errorcode.ErrorCode

doc {

	title "克隆云主机到指定物理机的回复"

	ref {
		name "error"
		path "org.zstack.header.vm.APICloneVmInstanceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "result"
		path "org.zstack.header.vm.APICloneVmInstanceEvent.result"
		desc "返回结果"
		type "CloneVmInstanceResults"
		since "0.6"
		clz CloneVmInstanceResults.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
}
