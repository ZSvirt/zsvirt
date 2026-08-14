package org.zstack.vpc

import org.zstack.header.vpc.VpcRouterVmInventory
import org.zstack.header.errorcode.ErrorCode

doc {

    title "创建路由表时提供可用的VPC路由器列表"

	ref {
		name "inventories"
		path "org.zstack.vpc.APIGetRouteTableVpcVRouterCandidateReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz VpcRouterVmInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.vpc.APIGetRouteTableVpcVRouterCandidateReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
