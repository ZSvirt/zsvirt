package org.zstack.header.resourceattribute.entity

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.resourceattribute.entity.ResourceAttributeValueInventory

doc {

	title "创建自定义属性值结果清单"

	ref {
		name "error"
		path "org.zstack.header.resourceattribute.entity.CreateResourceAttributeResult.error"
		desc "错误码，若不为 null，则表示操作失败，操作成功时该字段为 null"
		type "ErrorCode"
		since "zsv 4.10.16"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.resourceattribute.entity.CreateResourceAttributeResult.inventory"
		desc "自定义属性值结果。如果成功创建，则该值不为空"
		type "ResourceAttributeValueInventory"
		since "zsv 4.10.16"
		clz ResourceAttributeValueInventory.class
	}
	field {
		name "success"
		desc "创建是否成功"
		type "boolean"
		since "zsv 4.10.16"
	}
}
