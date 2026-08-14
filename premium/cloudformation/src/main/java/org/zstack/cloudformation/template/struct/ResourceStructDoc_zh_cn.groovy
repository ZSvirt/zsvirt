package org.zstack.cloudformation.template.struct

doc {

	title "资源编排解析后的资源列表"

	field {
		name "resourceName"
		desc "资源编排模板中的资源名称"
		type "String"
		since "3.0.0"
	}
	field {
		name "resourceType"
		desc "ZStack中的资源类型"
		type "String"
		since "3.0.0"
	}
	field {
		name "deletePolicy"
		desc "删除策略"
		type "String"
		since "3.0.0"
	}
	field {
		name "description"
		desc "资源编排模板中的资源描述"
		type "String"
		since "3.0.0"
	}
	field {
		name "inDegree"
		desc "依赖的资源列表"
		type "Set"
		since "3.0.0"
	}
	field {
		name "action"
		desc "后续的操作行为"
		type "String"
		since "3.0.0"
	}
	field {
		name "properties"
		desc "操作参数"
		type "Map"
		since "3.0.0"
	}
	field {
		name "results"
		desc "操作完成后的结果，若还未执行操作，则为空"
		type "Object"
		since "3.0.0"
	}
	ref {
		name "type"
		path "org.zstack.cloudformation.template.struct.ResourceStruct.type"
		desc "资源类型，Resource或Action"
		type "ResourceType"
		since "3.0.0"
		clz ResourceType.class
	}
	field {
		name "created"
		desc "是否己创建"
		type "boolean"
		since "3.0.0"
	}
	field {
		name "mockFailed"
		desc "测试用于mock失败"
		type "boolean"
		since "3.0.0"
	}
}
