package org.zstack.header.volume



doc {

	title "获取云盘格式"

	field {
		name "format"
		desc "镜像格式"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "masterHypervisorType"
		desc "默认的Hypervisor类型"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "supportingHypervisorTypes"
		desc "支持的Hypervisor类型列表"
		type "List"
		since "zsv 0.6"
	}
}
