package org.zstack.header.storage.volume.backup

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storage.backup.VolumeBackupInventory

doc {

	title "将卷备份同步至目标服务器结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.volume.backup.APISyncBackupFromImageStoreBackupStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.storage.volume.backup.APISyncBackupFromImageStoreBackupStorageEvent.inventory"
		desc "null"
		type "VolumeBackupInventory"
		since "2.6"
		clz VolumeBackupInventory.class
	}
}
