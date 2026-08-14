package org.zstack.network.l2.vxlan.vtep

import org.zstack.header.errorcode.ErrorCode
import org.zstack.network.l2.vxlan.vtep.VtepInventory

doc {

	title "VXLAN隧道端点清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.network.l2.vxlan.vtep.APICreateVxlanVtepEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.network.l2.vxlan.vtep.APICreateVxlanVtepEvent.inventory"
		desc "null"
		type "VtepInventory"
		since "3.0"
		clz VtepInventory.class
	}
}
