package org.zstack.guesttools

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "增强工具清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.7.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.7.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.7.0"
	}
	field {
		name "managementNodeUuid"
		desc "管理节点UUID"
		type "String"
		since "3.7.0"
	}
	field {
		name "architecture"
		desc "架构"
		type "String"
		since "3.7.0"
	}
	field {
		name "hypervisorType"
		desc "虚拟化类型"
		type "String"
		since "3.7.0"
	}
	field {
		name "version"
		desc "版本"
		type "String"
		since "3.7.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.7.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.7.0"
	}
}
