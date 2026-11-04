package org.zstack.header.storage.snapshot

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storage.snapshot.ShrinkResult

doc {

	title "快照瘦身返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.snapshot.APIShrinkVolumeSnapshotEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_3.10"
		clz ErrorCode.class
	}
	ref {
		name "shrinkResult"
		path "org.zstack.header.storage.snapshot.APIShrinkVolumeSnapshotEvent.shrinkResult"
		desc "null"
		type "ShrinkResult"
		since "zsv_3.10"
		clz ShrinkResult.class
	}
}
