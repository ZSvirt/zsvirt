package org.zstack.header.storage.volume.backup

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除卷备份结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.volume.backup.APIDeleteVolumeBackupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.6"
		clz ErrorCode.class
	}
}
