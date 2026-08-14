package org.zstack.zsv.core.entity



doc {

	title "管理节点的角色信息"

	field {
		name "uuid"
		desc "管理节点的 UUID，唯一标示该资源"
		type "String"
		since "4.10.7"
	}
	field {
		name "compute"
		desc "是否是计算节点"
		type "boolean"
		since "4.10.7"
	}
}
