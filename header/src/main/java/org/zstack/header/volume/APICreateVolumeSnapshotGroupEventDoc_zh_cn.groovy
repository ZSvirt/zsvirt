package org.zstack.header.volume

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory

doc {

	title "创建云盘快照组结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.volume.APICreateVolumeSnapshotGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 3.6.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.volume.APICreateVolumeSnapshotGroupEvent.inventory"
		desc "快照组清单"
		type "VolumeSnapshotGroupInventory"
		since "zsv 3.6.0"
		clz VolumeSnapshotGroupInventory.class
	}
}
