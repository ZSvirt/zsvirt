package org.zstack.header.vm

import org.zstack.header.vm.TemplatedVmInstanceInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新虚拟机模板返回"

	ref {
		name "inventory"
		path "org.zstack.header.vm.APIUpdateTemplatedVmInstanceEvent.inventory"
		desc "null"
		type "TemplatedVmInstanceInventory"
		since "zsv 4.2.6"
		clz TemplatedVmInstanceInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.2.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIUpdateTemplatedVmInstanceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.2.6"
		clz ErrorCode.class
	}
}
