package org.zstack.header.resourceattribute.api

import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "创建自定义属性键的结果"

	ref {
		name "inventory"
		path "org.zstack.header.resourceattribute.api.APICreateResourceAttributeKeyEvent.inventory"
		desc "自定义属性键清单"
		type "ResourceAttributeKeyInventory"
		since "zsv 4.10.16"
		clz ResourceAttributeKeyInventory.class
	}
	field {
		name "success"
		desc "创建是否成功"
		type "boolean"
		since "zsv 4.10.16"
	}
	ref {
		name "error"
		path "org.zstack.header.resourceattribute.api.APICreateResourceAttributeKeyEvent.error"
		desc "错误码，若不为 null，则表示操作失败，操作成功时该字段为 null"
		type "ErrorCode"
		since "zsv 4.10.16"
		clz ErrorCode.class
	}
}
