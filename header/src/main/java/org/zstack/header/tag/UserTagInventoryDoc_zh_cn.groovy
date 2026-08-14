package org.zstack.header.tag

import org.zstack.header.tag.TagPatternInventory
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "用户标签清单"

	field {
		name "tagPatternUuid"
		desc "标签模版UUID"
		type "String"
		since "zsv 3.2.0"
	}
	ref {
		name "tagPattern"
		path "org.zstack.header.tag.UserTagInventory.tagPattern"
		desc "以该标签模版创建的用户标签"
		type "TagPatternInventory"
		since "zsv 3.2.0"
		clz TagPatternInventory.class
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "resourceUuid"
		desc "用户指定的资源UUID，若指定，系统不会为该资源随机分配UUID"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "resourceType"
		desc "当创建一个标签时, 用户必须制定标签所关联的资源类型(resource type)"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "tag"
		desc "标签字符串"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "type"
		desc "保留域, 请不要使用它"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 0.6"
	}
}
