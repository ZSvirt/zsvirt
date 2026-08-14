package org.zstack.header.storage.database.backup

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storage.backup.SyncBackupResult

doc {

	title "扫描数据库备份数据"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.database.backup.APISyncDatabaseBackupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "result"
		path "org.zstack.header.storage.volume.backup.APISyncVmBackupEvent.result"
		desc "备份同步结果"
		type "SyncBackupResult"
		since "3.8.0"
		clz SyncBackupResult.class
	}
}
