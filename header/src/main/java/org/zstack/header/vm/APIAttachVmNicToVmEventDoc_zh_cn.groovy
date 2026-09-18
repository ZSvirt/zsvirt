package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.VmInstanceInventory

doc {

	title "云主机清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIAttachVmNicToVmEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_4.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.vm.APIAttachVmNicToVmEvent.inventory"
		desc "null"
		type "VmInstanceInventory"
		since "zsv_4.0"
		clz VmInstanceInventory.class
	}
}
