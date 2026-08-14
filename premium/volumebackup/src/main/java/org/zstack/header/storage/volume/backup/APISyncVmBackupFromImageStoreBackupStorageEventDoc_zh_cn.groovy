package org.zstack.header.storage.volume.backup

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storage.backup.VolumeBackupInventory

doc {

	title "同步虚拟机备份至备份服务器"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.volume.backup.APISyncVmBackupFromImageStoreBackupStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.0.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.storage.volume.backup.APISyncVmBackupFromImageStoreBackupStorageEvent.inventories"
		desc "null"
		type "List"
		since "3.0.0"
		clz VolumeBackupInventory.class
	}
}
