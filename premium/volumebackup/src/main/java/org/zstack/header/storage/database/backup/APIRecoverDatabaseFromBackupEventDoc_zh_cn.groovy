package org.zstack.header.storage.database.backup

import org.zstack.header.errorcode.ErrorCode

doc {

	title "从数据库备份恢复数据库(APIRecoverDatabaseFromBackupEvent)"

	ref {
		name "error"
		path "org.zstack.header.storage.database.backup.APIRecoverDatabaseFromBackupEvent.error"
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
		name "logListenPort"
		desc "浏览器可以通过此端口号实时打印日志"
		type "int"
		since "3.0.0"
	}
}
