package org.zstack.header.storage.snapshot.group

import org.zstack.header.errorcode.ErrorCode

doc {
	title "快照组树清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "5.2.0"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.snapshot.group.APIGetVolumeSnapshotGroupTreeReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.2.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.storage.snapshot.group.APIGetVolumeSnapshotGroupTreeReply.inventories"
		desc "指定云主机的快照组树清单"
		type "List"
		since "5.2.0"
		clz VolumeSnapshotGroupTreeInventory.class
	}
}
