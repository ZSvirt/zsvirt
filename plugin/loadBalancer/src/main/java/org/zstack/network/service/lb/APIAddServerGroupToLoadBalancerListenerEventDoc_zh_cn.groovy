package org.zstack.network.service.lb

import org.zstack.header.errorcode.ErrorCode
import org.zstack.network.service.lb.LoadBalancerListenerInventory

doc {

	title "负载均衡器监听器清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.network.service.lb.APIAddServerGroupToLoadBalancerListenerEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.network.service.lb.APIAddServerGroupToLoadBalancerListenerEvent.inventory"
		desc "null"
		type "LoadBalancerListenerInventory"
		since "4.0"
		clz LoadBalancerListenerInventory.class
	}
}
