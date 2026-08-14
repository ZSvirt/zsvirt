package org.zstack.resourceconfig

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "资源高级设置"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.4.0"
	}
	field {
		name "resourceUuid"
		desc "设置对应的资源UUID"
		type "String"
		since "3.4.0"
	}
	field {
		name "resourceType"
		desc "设置对应的资源类型"
		type "String"
		since "3.4.0"
	}
	field {
		name "name"
		desc "设置名称"
		type "String"
		since "3.4.0"
	}
	field {
		name "description"
		desc "设置的详细描述"
		type "String"
		since "3.4.0"
	}
	field {
		name "category"
		desc "设置类别"
		type "String"
		since "3.4.0"
	}
	field {
		name "value"
		desc "设置的值"
		type "String"
		since "3.4.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.4.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.4.0"
	}
}
