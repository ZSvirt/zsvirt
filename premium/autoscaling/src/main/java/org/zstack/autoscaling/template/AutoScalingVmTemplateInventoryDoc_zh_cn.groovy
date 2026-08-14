package org.zstack.autoscaling.template

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "伸缩组云主机模板详细信息"

	field {
		name "vmInstanceName"
		desc "云主机名称"
		type "String"
		since "3.1.0"
	}
	field {
		name "vmInstanceType"
		desc "云主机类型"
		type "String"
		since "3.1.0"
	}
	field {
		name "vmInstanceDescription"
		desc "云主机描述"
		type "String"
		since "3.1.0"
	}
	field {
		name "vmInstanceOfferingUuid"
		desc "云主机实例规格"
		type "String"
		since "3.1.0"
	}
	field {
		name "imageUuid"
		desc "云主机镜像UUID"
		type "String"
		since "3.1.0"
	}
	field {
		name "l3NetworkUuids"
		desc "云主机三层网络列表"
		type "List"
		since "3.1.0"
	}
	field {
		name "rootDiskOfferingUuid"
		desc "云主机根云盘规格"
		type "String"
		since "3.1.0"
	}
	field {
		name "dataDiskOfferingUuids"
		desc "云主机数据盘规格列表"
		type "List"
		since "3.1.0"
	}
	field {
		name "vmInstanceZoneUuid"
		desc "云主机所属区域"
		type "String"
		since "3.1.0"
	}
	field {
		name "vmInstanceClusterUuid"
		desc "云主机所属集群"
		type "String"
		since "3.1.0"
	}
	field {
		name "hostUuid"
		desc "云主机物理机UUID"
		type "String"
		since "3.1.0"
	}
	field {
		name "primaryStorageUuidForRootVolume"
		desc "云主机根云盘所在主存储"
		type "String"
		since "3.1.0"
	}
	field {
		name "defaultL3NetworkUuid"
		desc "云主机默认三层网络"
		type "String"
		since "3.1.0"
	}
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
