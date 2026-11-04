package org.zstack.storage.ceph.primary

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "CephOsdGroup结构体"

	field {
		name "primaryStorageUuid"
		desc "主存储UUID"
		type "String"
		since "zsv_0.6"
	}
	field {
		name "osds"
		desc ""
		type "String"
		since "zsv_0.6"
	}
	field {
		name "availableCapacity"
		desc ""
		type "long"
		since "zsv_0.6"
	}
	field {
		name "availablePhysicalCapacity"
		desc ""
		type "long"
		since "zsv_0.6"
	}
	field {
		name "totalPhysicalCapacity"
		desc ""
		type "long"
		since "zsv_0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv_0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv_0.6"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv_0.6"
	}
}
