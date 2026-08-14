package org.zstack.header.storage.volume.backup

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storage.backup.SyncBackupResult

doc {

	title "扫描云主机备份结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.volume.backup.APISyncVmBackupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.5.0"
		clz ErrorCode.class
	}
	ref {
		name "result"
		path "org.zstack.header.storage.volume.backup.APISyncVmBackupEvent.result"
		desc "null"
		type "SyncBackupResult"
		since "3.7.0"
		clz SyncBackupResult.class
	}
}
