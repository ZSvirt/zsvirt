package org.zstack.header.resourceattribute.entity

import java.sql.Timestamp

doc {

	title "自定义属性键清单"

	field {
		name "uuid"
		desc "自定义属性键 UUID"
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "name"
		desc "自定义属性键名称"
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 4.10.16"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 4.10.16"
	}
}
