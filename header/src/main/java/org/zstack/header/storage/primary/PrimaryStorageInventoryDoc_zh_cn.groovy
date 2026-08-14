package org.zstack.header.storage.primary

import java.lang.Long
import java.lang.Long
import java.lang.Long
import java.lang.Long
import java.lang.Long
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
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "url"
		desc ""
		type "String"
		since "zsv 0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "totalCapacity"
		desc ""
		type "Long"
		since "zsv 0.6"
	}
	field {
		name "availableCapacity"
		desc ""
		type "Long"
		since "zsv 0.6"
	}
	field {
		name "totalPhysicalCapacity"
		desc ""
		type "Long"
		since "zsv 0.6"
	}
	field {
		name "availablePhysicalCapacity"
		desc ""
		type "Long"
		since "zsv 0.6"
	}
	field {
		name "systemUsedCapacity"
		desc ""
		type "Long"
		since "zsv 0.6"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "zsv 0.6"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "zsv 0.6"
	}
	field {
		name "status"
		desc ""
		type "String"
		since "zsv 0.6"
	}
	field {
		name "mountPath"
		desc ""
		type "String"
		since "zsv 0.6"
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
	field {
		name "attachedClusterUuids"
		desc ""
		type "List"
		since "zsv 0.6"
	}
}
