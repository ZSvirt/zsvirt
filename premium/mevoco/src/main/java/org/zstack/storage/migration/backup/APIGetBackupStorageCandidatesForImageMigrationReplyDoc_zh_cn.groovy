package org.zstack.storage.migration.backup

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storage.backup.BackupStorageInventory

doc {

	title "镜像迁移的可选备份存储列表"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.storage.migration.backup.APIGetBackupStorageCandidatesForImageMigrationReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.2"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.storage.migration.backup.APIGetBackupStorageCandidatesForImageMigrationReply.inventories"
		desc "镜像迁移的可选备份存储列表"
		type "List"
		since "2.2"
		clz BackupStorageInventory.class
	}
}
