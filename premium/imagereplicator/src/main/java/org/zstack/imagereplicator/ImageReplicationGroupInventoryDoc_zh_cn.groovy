package org.zstack.imagereplicator

import org.zstack.imagereplicator.ReplicationGroupState
import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.imagereplicator.ImageReplicationGroupBackupStorageRefInventory

doc {

	title "镜像复制组清单"

	field {
		name "uuid"
		desc "镜像复制组的UUID，唯一标示该资源"
		type "String"
		since "3.5"
	}
	field {
		name "name"
		desc "镜像复制组的名称"
		type "String"
		since "3.5"
	}
	field {
		name "description"
		desc "镜像复制组的详细描述"
		type "String"
		since "3.5"
	}
	ref {
		name "state"
		path "org.zstack.imagereplicator.ImageReplicationGroupInventory.state"
		desc "null"
		type "ReplicationGroupState"
		since "3.5"
		clz ReplicationGroupState.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.5"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.5"
	}
	ref {
		name "backupStorageRefs"
		path "org.zstack.imagereplicator.ImageReplicationGroupInventory.backupStorageRefs"
		desc "null"
		type "List"
		since "3.5"
		clz ImageReplicationGroupBackupStorageRefInventory.class
	}
}
