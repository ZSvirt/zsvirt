package org.zstack.header.storage.snapshot.group

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storage.snapshot.group.RevertSnapshotGroupResult

doc {

	title "从快照组恢复云主机结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.snapshot.group.APIRevertVmFromSnapshotGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 3.6.0"
		clz ErrorCode.class
	}
	ref {
		name "results"
		path "org.zstack.header.storage.snapshot.group.APIRevertVmFromSnapshotGroupEvent.results"
		desc "恢复快照组结果，对应组内每个快照的恢复结果"
		type "List"
		since "zsv 3.6.0"
		clz RevertSnapshotGroupResult.class
	}
}
