package org.zstack.header.storage.backup

import org.zstack.header.errorcode.ErrorCode

doc {

	title "挂载镜像服务器至某个区域(Zone)"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.backup.APIAttachBackupStorageToZoneEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.storage.backup.APIAttachBackupStorageToZoneEvent.inventory"
		desc "镜像服务器清单"
		type "BackupStorageInventory"
		since "zsv 0.6"
		clz BackupStorageInventory.class
	}
}
