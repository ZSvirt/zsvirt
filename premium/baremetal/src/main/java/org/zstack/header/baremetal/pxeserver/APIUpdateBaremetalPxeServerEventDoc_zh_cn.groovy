package org.zstack.header.baremetal.pxeserver

import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新PXE服务返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "APIUpdateBaremetalPxeServerEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.6.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "APIUpdateBaremetalPxeServerEvent.inventory"
		desc "PXE服务清单"
		type "BaremetalPxeServerInventory"
		since "2.6.0"
		clz BaremetalPxeServerInventory.class
	}
}
