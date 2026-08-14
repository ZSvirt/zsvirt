package org.zstack.vpc

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取VPC云路由分布式路由是否打开结果"

	ref {
		name "error"
		path "org.zstack.vpc.APIGetVpcVRouterDistributedRoutingEnabledReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	field {
		name "enabled"
		desc "获取VPC云路由分布式路由是否打开"
		type "boolean"
		since "2.3"
	}
}
