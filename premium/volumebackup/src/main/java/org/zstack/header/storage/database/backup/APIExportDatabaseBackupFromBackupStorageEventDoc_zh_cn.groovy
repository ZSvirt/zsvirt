package org.zstack.header.storage.database.backup

import org.zstack.header.errorcode.ErrorCode

doc {

	title "从备份服务器导出数据库备份(APIExportDatabaseBackupFromBackupStorageEvent)"

	ref {
		name "error"
		path "org.zstack.header.storage.database.backup.APIExportDatabaseBackupFromBackupStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.0.0"
		clz ErrorCode.class
	}

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	field {
		name "databaseBackupUrl"
		desc "被导出备份的访问地址"
		type "String"
		since "3.0.0"
	}
}
