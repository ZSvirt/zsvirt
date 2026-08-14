package org.zstack.header.storage.snapshot

import java.lang.Boolean
import org.zstack.header.storage.snapshot.VolumeSnapshotTree.SnapshotLeafInventory
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "volumeUuid"
		desc "云盘UUID"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "current"
		desc ""
		type "Boolean"
		since "zsv 0.6"
	}
	ref {
		name "tree"
		path "org.zstack.header.storage.snapshot.VolumeSnapshotTreeInventory.tree"
		desc "null"
		type "SnapshotLeafInventory"
		since "zsv 0.6"
		clz SnapshotLeafInventory.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 0.6"
	}
}
