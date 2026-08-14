package org.zstack.header.cloudformation

doc {

	title "资源编排模板的参数清单"

	field {
		name "paramName"
		desc "参数名称"
		type "String"
		since "2.5.0"
	}
	field {
		name "type"
		desc "参数类型"
		type "String"
		since "2.5.0"
	}
	field {
		name "defaultValue"
		desc "默认值"
		type "String"
		since "2.5.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "2.5.0"
	}
	field {
		name "noEcho"
		desc "是否在输出中显示"
		type "Boolean"
		since "2.5.0"
	}
	field {
		name "label"
		desc "前端显示名称"
		type "String"
		since "2.5.0"
	}
	field {
		name "constraintDescription"
		desc "若校验失败，返回内容"
		type "String"
		since "2.5.0"
	}
	field {
		name "resourceType"
		desc "若参数为ZStack资源，返回资源类型，否则返回null"
		type "String"
		since "2.5.0"
	}
}
