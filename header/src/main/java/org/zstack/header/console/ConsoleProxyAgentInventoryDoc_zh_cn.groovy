package org.zstack.header.console

doc {

	title "控制台代理Agent清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "managementIp"
		desc "管理节点IP"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "type"
		desc "类型"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "status"
		desc "状态（连接，断开）"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "state"
		desc "状态（启用，禁用）"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 0.6"
	}
}
