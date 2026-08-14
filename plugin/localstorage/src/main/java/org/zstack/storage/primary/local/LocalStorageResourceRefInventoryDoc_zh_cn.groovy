package org.zstack.storage.primary.local

import java.lang.Long
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "resourceUuid"
		desc ""
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
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "0.6"
	}
	field {
		name "size"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "resourceType"
		desc ""
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
}
