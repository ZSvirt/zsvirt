package org.zstack.storage.ceph.primary

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "primaryStorageUuid"
		desc "主存储UUID"
		type "String"
		since "0.6"
	}
	field {
		name "poolName"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "aliasName"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "availableCapacity"
		desc ""
		type "Long"
		since "2.3.2"
	}
	field {
		name "usedCapacity"
		desc ""
		type "Long"
		since "2.3.2"
	}
	field {
		name "replicatedSize"
		desc ""
		type "Integer"
		since "2.3.2"
	}
	field {
		name "totalCapacity"
		desc ""
		type "Long"
		since "3.1.0"
	}
}
