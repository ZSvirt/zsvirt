package org.zstack.header.storage.snapshot.group

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory

doc {

	title "更新云盘快照组信息结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.snapshot.group.APIUpdateVolumeSnapshotGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 3.6.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.storage.snapshot.group.APIUpdateVolumeSnapshotGroupEvent.inventory"
		desc "null"
		type "VolumeSnapshotGroupInventory"
		since "zsv 3.6.0"
		clz VolumeSnapshotGroupInventory.class
	}
}
