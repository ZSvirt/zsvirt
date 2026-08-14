package org.zstack.autoscaling.group.activity

import java.sql.Timestamp
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "伸缩组活动详细信息"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.1.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.1.0"
	}
	field {
		name "scalingGroupUuid"
		desc "伸缩组UUID"
		type "String"
		since "3.1.0"
	}
	field {
		name "activityAction"
		desc "伸缩活动类型"
		type "String"
		since "3.1.0"
	}
	field {
		name "scalingGroupRuleUuid"
		desc "伸缩规则UUID"
		type "String"
		since "3.1.0"
	}
	field {
		name "cause"
		desc "触发伸缩活动的原因"
		type "String"
		since "3.1.0"
	}
	field {
		name "description"
		desc "活动详细描述"
		type "String"
		since "3.1.0"
	}
	field {
		name "status"
		desc "伸缩活动状态"
		type "String"
		since "3.1.0"
	}
	field {
		name "activityActionResultMessage"
		desc "伸缩活动执行结果"
		type "String"
		since "3.1.0"
	}
	field {
		name "endDate"
		desc "伸缩活动执行结束时间"
		type "Timestamp"
		since "3.1.0"
	}
	field {
		name "createDate"
		desc "伸缩活动创建时间"
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
