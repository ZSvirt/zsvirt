package org.zstack.header.storage.snapshot.group

import org.zstack.header.storage.snapshot.VolumeSnapshotInventory

doc {

	title "虚拟机快照组成员盘清单"

	field {
		name "volumeUuid"
		desc "云盘UUID"
		type "String"
		since "5.2.0"
	}
	field {
		name "volumeName"
		desc "云盘名称"
		type "String"
		since "5.2.0"
	}
	field {
		name "volumeType"
		desc "云盘类型（Root/Data）"
		type "String"
		since "5.2.0"
	}
	field {
		name "volumeSnapshotUuid"
		desc "对应的云盘快照UUID"
		type "String"
		since "5.2.0"
	}
	field {
		name "snapshotDeleted"
		desc "对应的云盘快照是否已被删除"
		type "boolean"
		since "5.2.0"
	}
	ref {
		name "snapshot"
		path "org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupTreeRefInventory.snapshot"
		desc "对应的云盘快照清单（被删除时为null，无访问权限时仅返回uuid）"
		type "VolumeSnapshotInventory"
		since "5.2.0"
		clz VolumeSnapshotInventory.class
	}
}
