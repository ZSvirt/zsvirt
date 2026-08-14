package org.zstack.templateConfig

import org.zstack.header.errorcode.ErrorCode
import org.zstack.templateConfig.TemplateConfigInventory

doc {

	title "更新模板值返回信息"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.templateConfig.APIUpdateTemplateConfigEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.6.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.templateConfig.APIUpdateTemplateConfigEvent.inventory"
		desc "null"
		type "TemplateConfigInventory"
		since "3.6.0"
		clz TemplateConfigInventory.class
	}
}
