package org.zstack.vpc

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vpc.VpcRouterVmInventory

doc {

	title "VPC云路由清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.vpc.APIAddDnsToVpcRouterEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.4"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.vpc.APIAddDnsToVpcRouterEvent.inventory"
		desc "null"
		type "VpcRouterVmInventory"
		since "2.4"
		clz VpcRouterVmInventory.class
	}
}
