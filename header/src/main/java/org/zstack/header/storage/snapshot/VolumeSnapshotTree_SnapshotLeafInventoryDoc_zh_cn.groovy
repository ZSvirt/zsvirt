package org.zstack.header.storage.snapshot

import org.zstack.header.storage.snapshot.VolumeSnapshotInventory
import org.zstack.header.storage.snapshot.VolumeSnapshotTree.SnapshotLeafInventory

doc {

	title "在这里输入结构的名称"

	ref {
		name "inventory"
		path "org.zstack.header.storage.snapshot.VolumeSnapshotTree.SnapshotLeafInventory.inventory"
		desc "null"
		type "VolumeSnapshotInventory"
		since "zsv 0.6"
		clz VolumeSnapshotInventory.class
	}
	field {
		name "parentUuid"
		desc ""
		type "String"
		since "zsv 0.6"
	}
	ref {
		name "children"
		path "org.zstack.header.storage.snapshot.VolumeSnapshotTree.SnapshotLeafInventory.children"
		desc "null"
		type "List"
		since "zsv 0.6"
		clz SnapshotLeafInventory.class
	}
}
