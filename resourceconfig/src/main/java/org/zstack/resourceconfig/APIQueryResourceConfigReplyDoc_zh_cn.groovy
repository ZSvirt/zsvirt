package org.zstack.resourceconfig

import org.zstack.header.errorcode.ErrorCode
import org.zstack.resourceconfig.ResourceConfigInventory

doc {

	title "查询资源高级设置的结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.resourceconfig.APIQueryResourceConfigReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.4.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.resourceconfig.APIQueryResourceConfigReply.inventories"
		desc "null"
		type "List"
		since "3.4.0"
		clz ResourceConfigInventory.class
	}
}
