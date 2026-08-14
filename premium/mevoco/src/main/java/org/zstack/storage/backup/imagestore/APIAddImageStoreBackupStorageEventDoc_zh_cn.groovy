package org.zstack.storage.backup.imagestore

import org.zstack.header.errorcode.ErrorCode

doc {

	title "ImageStore镜像服务器清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.storage.backup.imagestore.APIAddImageStoreBackupStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.storage.backup.imagestore.APIAddImageStoreBackupStorageEvent.inventory"
		desc "null"
		type "ImageStoreBackupStorageInventory"
		since "0.6"
		clz ImageStoreBackupStorageInventory.class
	}
}
