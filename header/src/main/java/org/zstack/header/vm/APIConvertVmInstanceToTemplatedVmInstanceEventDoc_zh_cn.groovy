package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.VmInstanceInventory

doc {

	title "虚拟机转换为虚拟机模板"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.2.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIConvertVmInstanceToTemplatedVmInstanceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 4.2.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.vm.APIConvertVmInstanceToTemplatedVmInstanceEvent.inventory"
		desc "虚拟机模板"
		type "TemplatedVmInstanceInventory"
		since "zsv 4.2.6"
		clz TemplatedVmInstanceInventory.class
	}
}
