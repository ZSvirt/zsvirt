package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.VmPriorityConfigInventory

doc {

	title "查询云主机优先级配置返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIQueryVmPriorityConfigReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 3.7"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.vm.APIQueryVmPriorityConfigReply.inventories"
		desc "null"
		type "List"
		since "zsv 3.7"
		clz VmPriorityConfigInventory.class
	}
}
