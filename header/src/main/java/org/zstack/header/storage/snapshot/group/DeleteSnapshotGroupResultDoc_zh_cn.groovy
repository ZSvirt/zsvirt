package org.zstack.header.storage.snapshot.group

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除快照组的结果"

	field {
		name "snapshotUuid"
		desc "快照UUID"
		type "String"
		since "zsv 3.6.0"
	}
	field {
		name "volumeUuid"
		desc "云盘UUID"
		type "String"
		since "zsv 3.6.0"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 3.6.0"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.snapshot.group.DeleteSnapshotGroupResult.error"
		desc "标识快照的删除结果，成功则为NULL"
		type "ErrorCode"
		since "zsv 3.6.0"
		clz ErrorCode.class
	}
}
