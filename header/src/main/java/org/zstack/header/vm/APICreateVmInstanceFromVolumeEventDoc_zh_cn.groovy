package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.VmInstanceInventory

doc {

	title "从云盘创建虚拟机结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APICreateVmInstanceFromVolumeEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 3.10.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.vm.APICreateVmInstanceFromVolumeEvent.inventory"
		desc "云主机实例"
		type "VmInstanceInventory"
		since "zsv 3.10.0"
		clz VmInstanceInventory.class
	}
}
