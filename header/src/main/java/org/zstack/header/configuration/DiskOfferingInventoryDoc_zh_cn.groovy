package org.zstack.header.configuration

doc {

	title "云盘规格清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "name"
		desc "资源名称"
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
		name "diskSize"
		desc "云盘大小"
		type "Long"
		since "zsv 0.6"
	}
	field {
		name "sortKey"
		desc ""
		type "Integer"
		since "zsv 0.6"
	}
	field {
		name "state"
		desc "状态（启动，禁用）"
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
	field {
		name "allocatorStrategy"
		desc "分配策略"
		type "String"
		since "zsv 0.6"
	}
}
