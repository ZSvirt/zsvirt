package org.zstack.network.service.lb

import org.zstack.header.errorcode.ErrorCode
import org.zstack.network.service.lb.LoadBalancerServerGroupInventory

doc {

	title "负载均衡器服务器组清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.network.service.lb.APICreateLoadBalancerServerGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.network.service.lb.APICreateLoadBalancerServerGroupEvent.inventory"
		desc "null"
		type "LoadBalancerServerGroupInventory"
		since "4.0"
		clz LoadBalancerServerGroupInventory.class
	}
}
