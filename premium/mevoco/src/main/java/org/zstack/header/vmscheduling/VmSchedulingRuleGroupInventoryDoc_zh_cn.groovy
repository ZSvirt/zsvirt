package org.zstack.header.vmscheduling

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "虚拟机调度组"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.16.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.16.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.16.0"
	}
	field {
		name "appliance"
		desc ""
		type "String"
		since "3.16.0"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "3.16.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.16.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.16.0"
	}
}
