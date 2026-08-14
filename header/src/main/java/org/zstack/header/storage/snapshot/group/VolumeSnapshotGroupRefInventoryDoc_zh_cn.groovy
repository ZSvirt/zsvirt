package org.zstack.header.storage.snapshot.group

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "快照组和快照所属关系"

	field {
		name "volumeSnapshotUuid"
		desc "云盘快照UUID"
		type "String"
		since "zsv 3.6.0"
	}
	field {
		name "volumeSnapshotGroupUuid"
		desc "快照组UUID"
		type "String"
		since "zsv 3.6.0"
	}
	field {
		name "deviceId"
		desc "打快照时云盘的加载序号"
		type "int"
		since "zsv 3.6.0"
	}
	field {
		name "snapshotDeleted"
		desc "快照是否已经被删除"
		type "boolean"
		since "zsv 3.6.0"
	}
	field {
		name "volumeUuid"
		desc "云盘UUID"
		type "String"
		since "zsv 3.6.0"
	}
	field {
		name "volumeName"
		desc "云盘的名字"
		type "String"
		since "zsv 3.6.0"
	}
	field {
		name "volumeType"
		desc "云盘的类型"
		type "String"
		since "zsv 3.6.0"
	}
	field {
		name "volumeSnapshotInstallPath"
		desc "快照的安装路径"
		type "String"
		since "zsv 3.6.0"
	}
	field {
		name "volumeSnapshotName"
		desc "快照的名字"
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
}
