package org.zstack.header.storage.volume.backup

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storage.backup.VolumeBackupInventory

doc {

	title "从目标服务器恢复卷备份结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.volume.backup.APIRecoverBackupFromImageStoreBackupStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.storage.volume.backup.APIRecoverBackupFromImageStoreBackupStorageEvent.inventory"
		desc "卷备份清单"
		type "VolumeBackupInventory"
		since "2.6"
		clz VolumeBackupInventory.class
	}
}
