package org.zstack.storage.backup.imagestore

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.image.ImageInventory

doc {

	title "镜像清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.storage.backup.imagestore.APIRecoveryImageFromImageStoreBackupStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.2"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.storage.backup.imagestore.APIRecoveryImageFromImageStoreBackupStorageEvent.inventory"
		desc "镜像属性"
		type "ImageInventory"
		since "2.2"
		clz ImageInventory.class
	}
}
