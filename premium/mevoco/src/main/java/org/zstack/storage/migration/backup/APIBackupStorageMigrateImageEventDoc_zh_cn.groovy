package org.zstack.storage.migration.backup

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.image.ImageInventory

doc {

	title "跨存储迁移镜像结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.storage.migration.backup.APIBackupStorageMigrateImageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.2"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.storage.migration.backup.APIBackupStorageMigrateImageEvent.inventory"
		desc "跨存储迁移所得镜像"
		type "ImageInventory"
		since "2.2"
		clz ImageInventory.class
	}
}
