package org.zstack.storage.primary.block

import java.lang.Integer
import java.lang.Long
import java.sql.Timestamp

doc {

	title "块设备主存储清单"

	field {
		name "vendorName"
		desc "存储厂商名称"
		type "String"
		since "3.15.11"
	}
	field {
		name "metadata"
		desc "存储元数据"
		type "String"
		since "3.15.11"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.15.11"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "3.15.11"
	}
	field {
		name "name"
		desc "主存储名称"
		type "String"
		since "3.15.11"
	}
	field {
		name "url"
		desc "未使用"
		type "String"
		since "3.15.11"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.15.11"
	}
	field {
		name "totalCapacity"
		desc "总空间"
		type "Long"
		since "3.15.11"
	}
	field {
		name "availableCapacity"
		desc "可用空间"
		type "Long"
		since "3.15.11"
	}
	field {
		name "totalPhysicalCapacity"
		desc "总物理空间"
		type "Long"
		since "3.15.11"
	}
	field {
		name "availablePhysicalCapacity"
		desc "物理可用空间"
		type "Long"
		since "3.15.11"
	}
	field {
		name "systemUsedCapacity"
		desc "系统使用空间"
		type "Long"
		since "3.15.11"
	}
	field {
		name "type"
		desc "主存储类型"
		type "String"
		since "3.15.11"
	}
	field {
		name "state"
		desc "启用状态"
		type "String"
		since "3.15.11"
	}
	field {
		name "status"
		desc "连接状态"
		type "String"
		since "3.15.11"
	}
	field {
		name "mountPath"
		desc "未使用"
		type "String"
		since "3.15.11"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.15.11"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.15.11"
	}
	field {
		name "attachedClusterUuids"
		desc "挂载的集群"
		type "List"
		since "3.15.11"
	}
}
