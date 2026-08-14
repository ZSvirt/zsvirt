package org.zstack.header.storage.volume.backup

import org.zstack.header.vm.VmInstanceInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "从备份创建云主机返回"

	ref {
		name "inventory"
		path "org.zstack.header.storage.volume.backup.APICreateVmFromVolumeBackupEvent.inventory"
		desc "云主机实例"
		type "VmInstanceInventory"
		since "3.18.0"
		clz VmInstanceInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.18.0"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.volume.backup.APICreateVmFromVolumeBackupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.18.0"
		clz ErrorCode.class
	}
}
