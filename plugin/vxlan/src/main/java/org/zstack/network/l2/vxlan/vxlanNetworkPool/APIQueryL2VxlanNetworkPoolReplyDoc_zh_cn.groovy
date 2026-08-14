package org.zstack.network.l2.vxlan.vxlanNetworkPool

import org.zstack.header.errorcode.ErrorCode
import org.zstack.network.l2.vxlan.vxlanNetworkPool.L2VxlanNetworkPoolInventory

doc {

	title "VXLAN资源池清单列表"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.network.l2.vxlan.vxlanNetworkPool.APIQueryL2VxlanNetworkPoolReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.network.l2.vxlan.vxlanNetworkPool.APIQueryL2VxlanNetworkPoolReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz L2VxlanNetworkPoolInventory.class
	}
}
