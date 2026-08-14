package org.zstack.header.cloudformation

doc {

	title "资源编排模板事件清单"

	field {
		name "id"
		desc "事件id"
		type "long"
		since "2.5.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "2.5.0"
	}
	field {
		name "action"
		desc "事件名称"
		type "String"
		since "2.5.0"
	}
	field {
		name "content"
		desc "事件参数列表"
		type "String"
		since "2.5.0"
	}
	field {
		name "resourceName"
		desc "资源名称"
		type "String"
		since "2.5.0"
	}
	field {
		name "actionStatus"
		desc "执行状态"
		type "String"
		since "2.5.0"
	}
	field {
		name "stackUuid"
		desc "堆栈UUID"
		type "String"
		since "2.5.0"
	}
	field {
		name "duration"
		desc "事件持续时间"
		type "String"
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
