package org.zstack.header.scheduler

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "name"
		desc "资源名称"
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
		name "schedulerType"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "schedulerInterval"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "repeatCount"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "startTime"
		desc ""
		type "Timestamp"
		since "0.6"
	}
	field {
		name "stopTime"
		desc ""
		type "Timestamp"
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
		name "jobsUuid"
		desc ""
		type "List"
		since "0.6"
	}
}
