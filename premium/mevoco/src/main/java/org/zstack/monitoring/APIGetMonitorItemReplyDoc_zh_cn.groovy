package org.zstack.monitoring

import org.zstack.header.errorcode.ErrorCode
import org.zstack.monitoring.items.ItemInventory

doc {

	title "获取报警条目返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.monitoring.APIGetMonitorItemReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.monitoring.APIGetMonitorItemReply.inventories"
		desc "null"
		type "List"
		since "2.1"
		clz ItemInventory.class
	}
}
