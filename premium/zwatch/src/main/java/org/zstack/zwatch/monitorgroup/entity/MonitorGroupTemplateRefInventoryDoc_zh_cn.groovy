package org.zstack.zwatch.monitorgroup.entity

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "资源分组应用的监控模板"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.10.0"
	}
	field {
		name "templateUuid"
		desc "模板UUID"
		type "String"
		since "3.10.0"
	}
	field {
		name "groupUuid"
		desc "资源分组UUID"
		type "String"
		since "3.10.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.10.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.10.0"
	}
	field {
		name "isApplied"
		desc "告警模板修改是否应用"
		type "Boolean"
		since "3.17.11"
	}
}
