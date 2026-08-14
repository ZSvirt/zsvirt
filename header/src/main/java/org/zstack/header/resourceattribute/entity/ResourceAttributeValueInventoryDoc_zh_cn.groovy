package org.zstack.header.resourceattribute.entity

import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyInventory
import java.sql.Timestamp

doc {

	title "自定义属性值清单"

	field {
		name "keyUuid"
		desc "自定义属性键 UUID"
		type "String"
		since "zsv 4.10.16"
	}
	ref {
		name "key"
		path "org.zstack.header.resourceattribute.entity.ResourceAttributeValueInventory.key"
		desc "对应的自定义属性键清单"
		type "ResourceAttributeKeyInventory"
		since "zsv 4.10.16"
		clz ResourceAttributeKeyInventory.class
	}
	field {
		name "value"
		desc "值"
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "resourceUuid"
		desc "资源 UUID"
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "resourceType"
		desc "资源类型"
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 4.10.16"
	}
}
