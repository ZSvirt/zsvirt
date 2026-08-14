package org.zstack.guesttools

import org.zstack.guesttools.GuestToolsStateInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "虚拟机 GuestTools 状态信息"

	ref {
		name "inventory"
		path "org.zstack.guesttools.APIUpdateGuestToolsStateReply.inventory"
		desc "虚拟机 GuestTools 状态清单"
		type "GuestToolsStateInventory"
		since "3.16"
		clz GuestToolsStateInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.16"
	}
	ref {
		name "error"
		path "org.zstack.guesttools.APIUpdateGuestToolsStateReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.16"
		clz ErrorCode.class
	}
}
