package org.zstack.header.image

import org.zstack.header.errorcode.ErrorCode

doc {

	title "从云盘快照创建根云盘错误清单"

	field {
		name "backupStorageUuid"
		desc "镜像存储UUID"
		type "String"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.image.APICreateRootVolumeTemplateFromVolumeSnapshotEvent.Failure.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
}
