package org.zstack.header.storage.snapshot

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storage.snapshot.BatchDeleteVolumeSnapshotStruct
import org.zstack.header.errorcode.ErrorCode

doc {

	title "批量删除快照结果"

	ref {
		name "error"
		path "org.zstack.header.storage.snapshot.APIBatchDeleteVolumeSnapshotEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 3.3"
		clz ErrorCode.class
	}
	ref {
		name "results"
		path "org.zstack.header.storage.snapshot.APIBatchDeleteVolumeSnapshotEvent.results"
		desc "每个快照的删除结果"
		type "List"
		since "zsv 3.3"
		clz BatchDeleteVolumeSnapshotStruct.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 3.3"
	}
}
