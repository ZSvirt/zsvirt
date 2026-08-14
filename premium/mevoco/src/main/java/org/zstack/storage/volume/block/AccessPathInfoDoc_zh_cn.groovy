package org.zstack.storage.volume.block



doc {

	title "获取访问路径"

	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.17.11"
	}
	field {
		name "accessPathId"
		desc "访问路径id"
		type "String"
		since "3.17.11"
	}
	field {
		name "accessPathIqn"
		desc "访问路径iqn"
		type "String"
		since "3.17.11"
	}
	field {
		name "targetCount"
		desc "关联目标数量"
		type "Integer"
		since "3.17.11"
	}
	field {
		name "gatewayIps"
		desc "网关ip"
		type "List"
		since "3.17.11"
	}
}
