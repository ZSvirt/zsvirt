package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "虚拟机网卡清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIQueryVmNicReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
		type "ErrorCode"
		since "zsv_0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.vm.APIQueryVmNicReply.inventories"
		desc "null"
		type "List"
		since "zsv_0.6"
		clz VmNicInventory.class
	}
}
