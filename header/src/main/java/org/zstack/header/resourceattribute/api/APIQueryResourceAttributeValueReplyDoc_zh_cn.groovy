package org.zstack.header.resourceattribute.api

import org.zstack.header.resourceattribute.entity.ResourceAttributeValueInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询自定义属性值的结果"

	ref {
		name "inventories"
		path "org.zstack.header.resourceattribute.api.APIQueryResourceAttributeValueReply.inventories"
		desc "自定义属性值清单列表"
		type "List"
		since "zsv 4.10.16"
		clz ResourceAttributeValueInventory.class
	}
	field {
		name "success"
		desc "查询是否成功"
		type "boolean"
		since "zsv 4.10.16"
	}
	ref {
		name "error"
		path "org.zstack.header.resourceattribute.api.APIQueryResourceAttributeValueReply.error"
		desc "错误码，若不为 null，则表示操作失败，操作成功时该字段为 null"
		type "ErrorCode"
		since "zsv 4.10.16"
		clz ErrorCode.class
	}
}
