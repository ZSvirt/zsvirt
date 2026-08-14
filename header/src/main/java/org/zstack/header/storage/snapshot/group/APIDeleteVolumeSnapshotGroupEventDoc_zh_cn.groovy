package org.zstack.header.storage.snapshot.group

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storage.snapshot.group.DeleteSnapshotGroupResult

doc {

	title "删除快照组结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.snapshot.group.APIDeleteVolumeSnapshotGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 3.6.0"
		clz ErrorCode.class
	}
	ref {
		name "results"
		path "org.zstack.header.storage.snapshot.group.APIDeleteVolumeSnapshotGroupEvent.results"
		desc "删除快照组结果，对应组内每一个快照"
		type "List"
		since "zsv 3.6.0"
		clz DeleteSnapshotGroupResult.class
	}
}
