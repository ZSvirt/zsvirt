package org.zstack.network.service.lb

import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.network.service.lb.LoadBalancerListenerServerGroupRefInventory
import org.zstack.network.service.lb.LoadBalancerServerGroupServerIpInventory
import org.zstack.network.service.lb.LoadBalancerServerGroupVmNicRefInventory

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "0.6"
	}
	field {
		name "loadBalancerUuid"
		desc "负载均衡器UUID"
		type "String"
		since "0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "0.6"
	}
	ref {
		name "listenerServerGroupRefs"
		path "org.zstack.network.service.lb.LoadBalancerServerGroupInventory.listenerServerGroupRefs"
		desc "null"
		type "List"
		since "0.6"
		clz LoadBalancerListenerServerGroupRefInventory.class
	}
	ref {
		name "serverIps"
		path "org.zstack.network.service.lb.LoadBalancerServerGroupInventory.serverIps"
		desc "null"
		type "List"
		since "0.6"
		clz LoadBalancerServerGroupServerIpInventory.class
	}
	ref {
		name "vmNicRefs"
		path "org.zstack.network.service.lb.LoadBalancerServerGroupInventory.vmNicRefs"
		desc "null"
		type "List"
		since "0.6"
		clz LoadBalancerServerGroupVmNicRefInventory.class
	}
}
