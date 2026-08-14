package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "设置vm RDP开关"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APISetVmRDPEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.1.1"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.vm.APISetVmRDPEvent.inventory"
		desc "null"
		type "VmInstanceInventory"
		since "2.1.1"
		clz VmInstanceInventory.class
	}
}
