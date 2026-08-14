package org.zstack.storage.primary.sharedblock

import org.zstack.storage.primary.sharedblock.SharedBlockInventory
import org.zstack.storage.primary.sharedblock.SharedBlockGroupType
import java.lang.Long
import java.lang.Long
import java.lang.Long
import java.lang.Long
import java.lang.Long
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "共享块设备主存储清单"

	ref {
		name "sharedBlocks"
		path "org.zstack.storage.primary.sharedblock.SharedBlockGroupPrimaryStorageInventory.sharedBlocks"
		desc "相关联的共享块设备"
		type "List"
		since "2.3.2"
		clz SharedBlockInventory.class
	}
	ref {
		name "sharedBlockGroupType"
		path "org.zstack.storage.primary.sharedblock.SharedBlockGroupPrimaryStorageInventory.sharedBlockGroupType"
		desc "共享块设备组类型"
		type "SharedBlockGroupType"
		since "2.3.2"
		clz SharedBlockGroupType.class
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.3.2"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "2.3.2"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "2.3.2"
	}
	field {
		name "url"
		desc "未使用"
		type "String"
		since "2.3.2"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "2.3.2"
	}
	field {
		name "totalCapacity"
		desc "总空间"
		type "Long"
		since "2.3.2"
	}
	field {
		name "availableCapacity"
		desc "可用空间"
		type "Long"
		since "2.3.2"
	}
	field {
		name "totalPhysicalCapacity"
		desc "总物理空间"
		type "Long"
		since "2.3.2"
	}
	field {
		name "availablePhysicalCapacity"
		desc "物理可用空间"
		type "Long"
		since "2.3.2"
	}
	field {
		name "systemUsedCapacity"
		desc "系统使用空间"
		type "Long"
		since "2.3.2"
	}
	field {
		name "type"
		desc "主存储类型"
		type "String"
		since "2.3.2"
	}
	field {
		name "state"
		desc "启用状态"
		type "String"
		since "2.3.2"
	}
	field {
		name "status"
		desc "连接状态"
		type "String"
		since "2.3.2"
	}
	field {
		name "mountPath"
		desc "未使用"
		type "String"
		since "2.3.2"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.3.2"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.3.2"
	}
	field {
		name "attachedClusterUuids"
		desc "挂载的集群"
		type "List"
		since "2.3.2"
	}
}
