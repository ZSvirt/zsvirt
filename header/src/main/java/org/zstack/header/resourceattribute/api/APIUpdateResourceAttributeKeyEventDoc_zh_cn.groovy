package org.zstack.header.resourceattribute.api

import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新自定义属性键结果"

	ref {
		name "inventory"
		path "org.zstack.header.resourceattribute.api.APIUpdateResourceAttributeKeyEvent.inventory"
		desc "自定义属性键清单"
		type "ResourceAttributeKeyInventory"
		since "zsv 4.10.16"
		clz ResourceAttributeKeyInventory.class
	}
	field {
		name "success"
		desc "更新是否成功"
		type "boolean"
		since "zsv 4.10.16"
	}
	ref {
		name "error"
		path "org.zstack.header.resourceattribute.api.APIUpdateResourceAttributeKeyEvent.error"
		desc "错误码，若不为 null，则表示操作失败，操作成功时该字段为 null"
		type "ErrorCode"
		since "zsv 4.10.16"
		clz ErrorCode.class
	}
}
