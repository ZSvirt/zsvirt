package org.zstack.header.cluster

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "集群清单"

	field {
		name "name"
		desc "资源名称"
		type "String"
		since "zsv 0.6"
	}
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
		name "state"
		desc "集群状态"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "hypervisorType"
		desc "虚拟机管理程序类型"
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
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "type"
		desc "保留域"
		type "String"
		since "zsv 0.6"
	}
}
