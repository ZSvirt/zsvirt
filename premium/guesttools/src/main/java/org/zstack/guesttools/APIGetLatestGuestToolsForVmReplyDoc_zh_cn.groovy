package org.zstack.guesttools

import org.zstack.header.errorcode.ErrorCode
import org.zstack.guesttools.GuestToolsInventory

doc {

	title "获取云主机可用最新增强工具的返回"

	ref {
		name "error"
		path "org.zstack.guesttools.APIGetLatestGuestToolsForVmReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.7.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.guesttools.APIGetLatestGuestToolsForVmReply.inventory"
		desc "可用最新增强工具清单"
		type "GuestToolsInventory"
		since "3.7.0"
		clz GuestToolsInventory.class
	}
	field {
		name "success"
		desc "成功"
		type "boolean"
		since "3.7.0"
	}
}
