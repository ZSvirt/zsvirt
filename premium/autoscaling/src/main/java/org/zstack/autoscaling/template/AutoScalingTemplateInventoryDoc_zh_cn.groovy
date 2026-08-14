package org.zstack.autoscaling.template

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "伸缩组模板详细信息"

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
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.1.0"
	}
	field {
		name "type"
		desc "模板类型"
		type "String"
		since "3.1.0"
	}
	field {
		name "state"
		desc "模板启用状态"
		type "String"
		since "3.1.0"
	}
	field {
		name "systemTags"
		desc ""
		type "List"
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
