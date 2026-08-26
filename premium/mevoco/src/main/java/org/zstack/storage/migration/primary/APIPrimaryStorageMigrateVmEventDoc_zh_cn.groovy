package org.zstack.storage.migration.primary

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.VmInstanceInventory

doc {

	title "跨存储迁移虚拟机的结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVmEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 2.6.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVmEvent.inventory"
		desc "null"
		type "VmInstanceInventory"
		since "zsv 2.6.0"
		clz VmInstanceInventory.class
	}
}
