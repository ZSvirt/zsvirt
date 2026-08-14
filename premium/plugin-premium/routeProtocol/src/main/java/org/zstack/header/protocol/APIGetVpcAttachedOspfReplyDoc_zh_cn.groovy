package org.zstack.header.protocol

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.protocol.NetworkRouterAreaRefInventory

doc {

	title "获取VPC云路由已关联的Ospf"

	ref {
		name "error"
		path "org.zstack.header.protocol.APIGetVpcAttachedOspfReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.protocol.APIGetVpcAttachedOspfReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz NetworkRouterAreaRefInventory.class
	}
	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "0.6"
	}
}
