package org.zstack.header.protocol

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.protocol.NetworkRouterAreaRefInventory

doc {

	title "添加网络到OSPF区域中回复"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.protocol.APIAddVRouterNetworksToOspfAreaEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.4"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.protocol.APIAddVRouterNetworksToOspfAreaEvent.inventories"
		desc "路由器的清单"
		type "List"
		since "3.4"
		clz NetworkRouterAreaRefInventory.class
	}
}
