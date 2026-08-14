package org.zstack.header.storage.snapshot.group


import org.zstack.header.errorcode.ErrorCode

doc {

	title "检查内存快照组冲突信息返回"

	ref {
		name "vmNicConflict"
		path "org.zstack.header.storage.snapshot.group.APICheckMemorySnapshotGroupConflictReply.vmNicConflict"
		desc "null"
		type "List"
		since "zsv 4.10.0"
		clz VmNicConflictEntry.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.10.0"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.snapshot.group.APICheckMemorySnapshotGroupConflictReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 4.10.0"
		clz ErrorCode.class
	}
}
