package org.zstack.templateConfig

import org.zstack.header.errorcode.ErrorCode
import org.zstack.templateConfig.TemplateConfigInventory

doc {

	title "查询模板配置返回信息"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.templateConfig.APIQueryTemplateConfigReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.6.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.templateConfig.APIQueryTemplateConfigReply.inventories"
		desc "null"
		type "List"
		since "3.6.0"
		clz TemplateConfigInventory.class
	}
}
