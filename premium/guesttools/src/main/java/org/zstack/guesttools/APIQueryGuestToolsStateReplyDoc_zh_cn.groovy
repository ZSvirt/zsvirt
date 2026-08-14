package org.zstack.guesttools

import org.zstack.guesttools.GuestToolsStateInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取虚拟机 GuestTools 状态的请求返回"

	ref {
		name "inventories"
		path "org.zstack.guesttools.APIQueryGuestToolsStateReply.inventories"
		desc "GuestTools 状态清单列表"
		type "List"
		since "3.16.11"
		clz GuestToolsStateInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.16.11"
	}
	ref {
		name "error"
		path "org.zstack.guesttools.APIQueryGuestToolsStateReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.16.11"
		clz ErrorCode.class
	}
}
