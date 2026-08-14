package org.zstack.header.storage.snapshot.group

import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefInventory

doc {

	title "快照组清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 3.6.0"
	}
	field {
		name "snapshotCount"
		desc "组内快照数量"
		type "Integer"
		since "zsv 3.6.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "zsv 3.6.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "zsv 3.6.0"
	}
	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "zsv 3.6.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 3.6.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 3.6.0"
	}
	ref {
		name "volumeSnapshotRefs"
		path "org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory.volumeSnapshotRefs"
		desc "组内快照信息"
		type "List"
		since "zsv 3.6.0"
		clz VolumeSnapshotGroupRefInventory.class
	}
}
