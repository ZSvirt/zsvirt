package org.zstack.header.cloudformation

doc {

	title "资源编排模板清单"

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
		name "type"
		desc "模板类型，默认zstack"
		type "String"
		since "2.5.0"
	}
	field {
		name "version"
		desc "模板版本号"
		type "String"
		since "2.5.0"
	}
	field {
		name "state"
		desc "模板是否启用"
		type "Boolean"
		since "2.5.0"
	}
	field {
		name "content"
		desc "模板内容，json字符串"
		type "String"
		since "2.5.0"
	}
	field {
		name "md5sum"
		desc "content字段内容的md5校验值"
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
