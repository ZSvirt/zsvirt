package org.zstack.directory



doc {

	title "目录"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.16.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.16.0"
	}
	field {
		name "groupName"
		desc "所属分组"
		type "String"
		since "3.16.0"
	}
	field {
		name "parentUuid"
		desc "父级目录UUID"
		type "String"
		since "3.16.0"
	}
	field {
		name "rootDirectoryUuid"
		desc "根目录UUID"
		type "String"
		since "3.16.0"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "3.16.0"
	}
	field {
		name "type"
		desc "目录类型"
		type "String"
		since "3.16.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.16.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.16.0"
	}
}
