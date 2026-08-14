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
		name "schedulerJobUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "schedulerTriggerUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "jobGroup"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "triggerGroup"
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
