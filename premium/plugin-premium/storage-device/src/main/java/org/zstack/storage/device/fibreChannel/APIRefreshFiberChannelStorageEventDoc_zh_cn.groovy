package org.zstack.storage.device.fibreChannel

import org.zstack.header.errorcode.ErrorCode
import org.zstack.storage.device.fibreChannel.FiberChannelStorageInventory

doc {

	title "在这里输入结构的名称"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.storage.device.fibreChannel.APIRefreshFiberChannelStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.1.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.storage.device.fibreChannel.APIRefreshFiberChannelStorageEvent.inventories"
		desc "null"
		type "List"
		since "3.1.0"
		clz FiberChannelStorageInventory.class
	}
}
