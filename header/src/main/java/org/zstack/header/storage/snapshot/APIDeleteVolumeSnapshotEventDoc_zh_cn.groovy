package org.zstack.header.storage.snapshot

import org.zstack.header.errorcode.ErrorCode

doc {

	title "云盘快照删除结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
}
