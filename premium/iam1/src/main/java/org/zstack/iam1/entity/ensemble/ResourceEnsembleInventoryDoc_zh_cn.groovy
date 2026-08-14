package org.zstack.iam1.entity.ensemble

import org.zstack.header.vo.ResourceInventory

doc {

	title "资源组中的所有成员结果"

	field {
		name "masterUuid"
		desc "资源组中主要资源的UUID"
		type "String"
		since "4.10.0"
	}
	field {
		name "masterResourceName"
		desc "资源组中主要资源的名称"
		type "String"
		since "4.10.0"
	}
	field {
		name "masterResourceType"
		desc "资源组中主要资源的类型"
		type "String"
		since "4.10.0"
	}
	ref {
		name "members"
		path "org.zstack.iam1.entity.ensemble.ResourceEnsembleInventory.members"
		desc "资源组中除了主要资源，其它所有涉及的资源都罗列在这里"
		type "List"
		since "4.10.0"
		clz ResourceInventory.class
	}
}
