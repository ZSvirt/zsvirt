package org.zstack.autoscaling.group.rule.trigger

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "伸缩规则触发器详细信息"

	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.1.0"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.1.0"
	}
	field {
		name "type"
		desc "触发器类型"
		type "String"
		since "3.1.0"
	}
	field {
		name "ruleUuid"
		desc "伸缩规则UUID"
		type "String"
		since "3.1.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.1.0"
	}
	field {
		name "state"
		desc "触发器状态"
		type "String"
		since "3.1.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.1.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.1.0"
	}
}
