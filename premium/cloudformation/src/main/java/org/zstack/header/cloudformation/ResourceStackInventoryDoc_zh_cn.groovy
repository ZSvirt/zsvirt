package org.zstack.header.cloudformation

doc {

	title "资源编排堆栈清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.5.0"
	}
	field {
		name "name"
		desc "资源名称"
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
		name "version"
		desc "堆栈版本"
		type "String"
		since "2.5.0"
	}
	field {
		name "type"
		desc "堆栈类型，默认为zstack"
		type "String"
		since "2.5.0"
	}
	field {
		name "templateContent"
		desc "资堆栈内容，json字符串"
		type "String"
		since "2.5.0"
	}
	field {
		name "paramContent"
		desc "堆栈对应的参数列表，json字符串"
		type "String"
		since "2.5.0"
	}
	field {
		name "status"
		desc "堆栈状态"
		type "String"
		since "2.5.0"
	}
	field {
		name "reason"
		desc "堆栈创建失败的原因"
		type "String"
		since "2.5.0"
	}
	field {
		name "outputs"
		desc "堆栈创建后的输出字段"
		type "String"
		since "3.9.0"
	}
	field {
		name "enableRollback"
		desc "堆栈创建失败时是否回滚"
		type "boolean"
		since "2.5.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.5.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.5.0"
	}
}
