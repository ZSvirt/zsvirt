package org.zstack.autoscaling.group

import java.lang.Long
import java.lang.Integer
import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "伸缩组详细信息"

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
		name "scalingResourceType"
		desc "伸缩资源类型：云主机"
		type "String"
		since "3.1.0"
	}
	field {
		name "state"
		desc "伸缩组启用状态"
		type "String"
		since "3.1.0"
	}
	field {
		name "defaultCooldown"
		desc "伸缩规则默认冷却时间"
		type "Long"
		since "3.1.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.1.0"
	}
	field {
		name "minResourceSize"
		desc "伸缩组里最少云主机数量"
		type "Integer"
		since "3.1.0"
	}
	field {
		name "maxResourceSize"
		desc "伸缩组里最多云主机数量"
		type "Integer"
		since "3.1.0"
	}
	field {
		name "removalPolicy"
		desc "删除云主机默认策略"
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
		name "attachedTemplates"
		desc "挂载的伸缩组云主机模板列表"
		type "List"
		since "3.1.0"
	}
	field {
		name "systemTags"
		desc ""
		type "List"
		since "3.1.0"
	}
}
