package org.zstack.header.vm

import org.zstack.header.vm.VmInstanceInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "扁平合并虚拟机结果"

	ref {
		name "inventory"
		path "org.zstack.header.vm.APIFlattenVmInstanceEvent.inventory"
		desc "虚拟机清单"
		type "VmInstanceInventory"
		since "zsv 3.17.0"
		clz VmInstanceInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIFlattenVmInstanceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.17.0"
		clz ErrorCode.class
	}
}
