package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.VmInstanceInventory

doc {

	title "从快照创建虚拟机结果"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.11.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APICreateVmInstanceFromVolumeSnapshotEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.11.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.vm.APICreateVmInstanceFromVolumeSnapshotEvent.inventory"
		desc "虚拟机清单"
		type "VmInstanceInventory"
		since "zsv 3.11.0"
		clz VmInstanceInventory.class
	}
}
