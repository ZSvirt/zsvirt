package org.zstack.header.storage.snapshot.group

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupAvailability

doc {

	title "检查快照组可用性"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.snapshot.group.APICheckVolumeSnapshotGroupAvailabilityReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 3.6.0"
		clz ErrorCode.class
	}
	ref {
		name "results"
		path "org.zstack.header.storage.snapshot.group.APICheckVolumeSnapshotGroupAvailabilityReply.results"
		desc "快照组可用性结果"
		type "List"
		since "zsv 3.6.0"
		clz VolumeSnapshotGroupAvailability.class
	}
}
