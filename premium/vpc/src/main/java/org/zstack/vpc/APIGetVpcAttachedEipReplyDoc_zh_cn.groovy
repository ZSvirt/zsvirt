package org.zstack.vpc

import org.zstack.header.errorcode.ErrorCode
import org.zstack.network.service.eip.EipInventory

doc {

	title "获取VPC云路由已关联的弹性IP的结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.vpc.APIGetVpcAttachedEipReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.1"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.vpc.APIGetVpcAttachedEipReply.inventories"
		desc "null"
		type "List"
		since "4.1"
		clz EipInventory.class
	}
}
