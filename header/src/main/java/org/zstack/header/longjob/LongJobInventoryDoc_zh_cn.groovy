package org.zstack.header.longjob

doc {

	title "长任务"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 2.2.4"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "zsv 2.2.4"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "zsv 2.2.4"
	}
	field {
		name "apiId"
		desc "用于关联TaskProgress的APIID"
		type "String"
		since "zsv 2.2.4"
	}
	field {
		name "jobName"
		desc "任务名称"
		type "String"
		since "zsv 2.2.4"
	}
	field {
		name "jobData"
		desc "任务数据"
		type "String"
		since "zsv 2.2.4"
	}
	field {
		name "jobResult"
		desc "任务结果"
		type "String"
		since "zsv 2.2.4"
	}
	ref {
		name "state"
		path "org.zstack.header.longjob.LongJobInventory.state"
		desc "任务状态"
		type "LongJobState"
		since "zsv 2.2.4"
		clz LongJobState.class
	}
	field {
		name "targetResourceUuid"
		desc "目标资源UUID"
		type "String"
		since "zsv 2.2.4"
	}
	field {
		name "managementNodeUuid"
		desc "管理节点UUID"
		type "String"
		since "zsv 2.2.4"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 2.2.4"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 2.2.4"
	}
}
