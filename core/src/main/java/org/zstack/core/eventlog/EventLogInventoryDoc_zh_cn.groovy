package org.zstack.core.eventlog

import java.sql.Timestamp

doc {

	title "Event Log的数据结构"

	field {
		name "id"
		desc ""
		type "long"
		since "3.12.0"
	}
	field {
		name "content"
		desc ""
		type "String"
		since "3.12.0"
	}
	field {
		name "resourceUuid"
		desc "资源UUID"
		type "String"
		since "3.12.0"
	}
	field {
		name "resourceType"
		desc ""
		type "String"
		since "3.12.0"
	}
	field {
		name "category"
		desc ""
		type "String"
		since "3.12.0"
	}
	field {
		name "trackingId"
		desc ""
		type "String"
		since "3.12.0"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "3.12.0"
	}
	field {
		name "time"
		desc ""
		type "long"
		since "3.12.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.12.0"
	}
}
