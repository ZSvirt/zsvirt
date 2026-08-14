package org.zstack.header.storage.snapshot

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除快照结果"

	field {
		name "snapshotUuid"
		desc "快照UUID"
		type "String"
		since "zsv 3.3"
	}
	field {
		name "success"
		desc "是否成功"
		type "boolean"
		since "zsv 3.3"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.snapshot.BatchDeleteVolumeSnapshotStruct.error"
		desc ""
		type "ErrorCode"
		since "zsv 3.3"
		clz ErrorCode.class
	}
}
