package org.zstack.header.storage.database.backup

import org.zstack.header.errorcode.ErrorCode

doc {

    title "获取备份服务器上的数据库备份信息"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
    ref {
        name "error"
        path "org.zstack.header.storage.database.backup.APIGetDatabaseBackupFromImageStoreReply.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
        type "ErrorCode"
        since "3.0.0"
        clz ErrorCode.class
    }
    ref {
        name "backups"
        path "org.zstack.header.storage.database.backup.APIGetDatabaseBackupFromImageStoreReply.backups"
        desc "数据库备份信息列表"
        type "List"
        since "3.0.0"
        clz DatabaseBackupStruct.class
    }
}
