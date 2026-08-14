package org.zstack.network.l2.vxlan.vxlanNetworkPool

import org.zstack.network.l2.vxlan.vtep.RemoteVtepInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "创建远端 VXLAN 隧道端点结果"

	ref {
		name "inventory"
		path "org.zstack.network.l2.vxlan.vxlanNetworkPool.APICreateVxlanPoolRemoteVtepEvent.inventory"
		desc "远端 VXLAN 隧道端点清单"
		type "RemoteVtepInventory"
		since "3.17.11"
		clz RemoteVtepInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.11"
	}
	ref {
		name "error"
		path "org.zstack.network.l2.vxlan.vxlanNetworkPool.APICreateVxlanPoolRemoteVtepEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.11"
		clz ErrorCode.class
	}
}
