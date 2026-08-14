package org.zstack.log

import org.zstack.header.errorcode.ErrorCode
import org.zstack.core.jsonlabel.JsonLabelInventory

doc {

	title "添加日志服务器配置的结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.log.APIAddLogConfigurationEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.7.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.log.APIAddLogConfigurationEvent.inventory"
		desc "日志服务器配置"
		type "JsonLabelInventory"
		since "3.7.0"
		clz JsonLabelInventory.class
	}
}
