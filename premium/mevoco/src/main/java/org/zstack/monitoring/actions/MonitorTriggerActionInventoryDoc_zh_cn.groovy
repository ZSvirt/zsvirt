package org.zstack.monitoring.actions

doc {

	title "报警器动作清单"

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
		name "postScript"
		desc "保留字段"
		type "String"
		since "0.6"
	}
	field {
		name "state"
		desc "状态。Enabled/Disabled"
		type "String"
		since "0.6"
	}
	field {
		name "mediaUuids"
		desc "媒体UUIDs"
		type "List"
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
