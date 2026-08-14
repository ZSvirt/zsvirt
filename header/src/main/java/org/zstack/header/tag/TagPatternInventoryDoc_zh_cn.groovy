package org.zstack.header.tag

import org.zstack.header.tag.TagPatternType
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "标签模版"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 3.2.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "zsv 3.2.0"
	}
	field {
		name "value"
		desc "标签的值"
		type "String"
		since "zsv 3.2.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "zsv 3.2.0"
	}
	field {
		name "color"
		desc "标签的颜色"
		type "String"
		since "zsv 3.2.0"
	}
	ref {
		name "type"
		path "org.zstack.header.tag.TagPatternInventory.type"
		desc "标签的类型"
		type "TagPatternType"
		since "zsv 3.2.0"
		clz TagPatternType.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 3.2.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 3.2.0"
	}
}
