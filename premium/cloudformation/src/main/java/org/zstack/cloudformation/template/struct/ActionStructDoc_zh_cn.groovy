package org.zstack.cloudformation.template.struct

doc {

	title "资源编排行为清单"

	field {
		name "resourceName"
		desc "资源类型"
		type "String"
		since "2.5.0"
	}
	field {
		name "actionName"
		desc "行为名称"
		type "String"
		since "2.5.0"
	}
	field {
		name "round"
		desc "第几轮执行，从0开始计算"
		type "int"
		since "2.5.0"
	}
	field {
		name "inDegree"
		desc "依赖的行为列表"
		type "Set"
		since "2.5.0"
	}
	field {
		name "actions"
		desc "执行操作的参数列表"
		type "Object"
		since "2.5.0"
	}
	field {
		name "error"
		desc "本次执行的出现的错误"
		type "String"
		since "2.5.0"
	}
}
