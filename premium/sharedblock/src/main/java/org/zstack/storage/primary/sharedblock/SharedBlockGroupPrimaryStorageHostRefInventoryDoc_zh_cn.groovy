package org.zstack.storage.primary.sharedblock

import java.lang.Integer
import org.zstack.header.storage.primary.PrimaryStorageHostStatus
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "共享块设备主存储物理机连接状态清单"

	field {
		name "primaryStorageUuid"
		desc "主存储UUID"
		type "String"
		since "2.3.2"
	}
	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "2.3.2"
	}
	field {
		name "hostId"
		desc "物理机ID"
		type "Integer"
		since "2.3.2"
	}
	ref {
		name "status"
		path "org.zstack.storage.primary.sharedblock.SharedBlockGroupPrimaryStorageHostRefInventory.status"
		desc "null"
		type "PrimaryStorageHostStatus"
		since "2.3.2"
		clz PrimaryStorageHostStatus.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.3.2"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.3.2"
	}
}
