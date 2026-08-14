package org.zstack.header.storage.backup

import org.zstack.header.errorcode.ErrorCode

doc {

	title "从镜像服务器删除导出的镜像"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.backup.APIDeleteExportedImageFromBackupStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 1.7"
		clz ErrorCode.class
	}
}
