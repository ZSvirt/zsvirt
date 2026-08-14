package org.zstack.header.storage.database.backup

import org.zstack.header.errorcode.ErrorCode

doc {

	title "从备份服务器删除导出的数据库备份"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.database.backup.APIDeleteExportedDatabaseBackupFromBackupStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.0.0"
		clz ErrorCode.class
	}
}
