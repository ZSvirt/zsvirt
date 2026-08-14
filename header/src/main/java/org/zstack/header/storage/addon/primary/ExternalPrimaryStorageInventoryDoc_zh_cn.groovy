package org.zstack.header.storage.addon.primary

doc {

	title "外部主存储清单"

	field {
		name "identity"
		desc "存储标识"
		type "String"
		since "zsv 4.10.6"
	}
	field {
		name "config"
		desc "配置"
		type "String"
		since "zsv 4.10.6"
	}
	field {
		name "addonInfo"
		desc "额外信息"
		type "String"
		since "zsv 4.10.6"
	}
	field {
		name "outputProtocols"
		desc "可输出协议"
		type "List"
		since "zsv 4.10.6"
	}
	field {
		name "defaultProtocol"
		desc "默认协议"
		type "String"
		since "zsv 4.10.6"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 4.10.6"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "zsv 4.10.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "zsv 4.10.6"
	}
	field {
		name "url"
		desc ""
		type "String"
		since "zsv 4.10.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "zsv 4.10.6"
	}
	field {
		name "totalCapacity"
		desc ""
		type "Long"
		since "zsv 4.10.6"
	}
	field {
		name "availableCapacity"
		desc ""
		type "Long"
		since "zsv 4.10.6"
	}
	field {
		name "totalPhysicalCapacity"
		desc ""
		type "Long"
		since "zsv 4.10.6"
	}
	field {
		name "availablePhysicalCapacity"
		desc ""
		type "Long"
		since "zsv 4.10.6"
	}
	field {
		name "systemUsedCapacity"
		desc ""
		type "Long"
		since "zsv 4.10.6"
	}
	field {
		name "type"
		desc "类型"
		type "String"
		since "zsv 4.10.6"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "zsv 4.10.6"
	}
	field {
		name "status"
		desc ""
		type "String"
		since "zsv 4.10.6"
	}
	field {
		name "mountPath"
		desc ""
		type "String"
		since "zsv 4.10.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 4.10.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 4.10.6"
	}
	field {
		name "attachedClusterUuids"
		desc "绑定的集群 UUID 列表"
		type "List"
		since "zsv 4.10.6"
	}
}
