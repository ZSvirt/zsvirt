package org.zstack.header.cloudformation



doc {

	title "资源编排支持的资源清单"

	field {
		name "name"
		desc "资源名称"
		type "String"
		since "2.6.0"
	}
	field {
		name "type"
		desc "类型，可为Action或Resource"
		type "String"
		since "2.6.0"
	}
	field {
		name "actionName"
		desc "执行的操作名称"
		type "String"
		since "2.6.0"
	}
	field {
		name "resources"
		desc "关联到的资源列表"
		type "List"
		since "2.6.0"
	}
}
