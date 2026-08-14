package org.zstack.autoscaling.group.instance

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "伸缩组云主机详细信息"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.1.0"
	}
	field {
		name "instanceUuid"
		desc "云主机UUID"
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
		name "templateUuid"
		desc "伸缩组云主机模块UUID"
		type "String"
		since "3.1.0"
	}
	field {
		name "scalingGroupActivityUuid"
		desc "伸缩活动UUID"
		type "String"
		since "3.1.0"
	}
	field {
		name "status"
		desc "云主机在伸缩组里的状态"
		type "String"
		since "3.1.0"
	}
	field {
		name "healthStatus"
		desc "云主机在伸缩组里的健康状态"
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
	field {
		name "protectionStrategy"
		desc "实例保护策略"
		type "String"
		since "0.6"
	}
}
