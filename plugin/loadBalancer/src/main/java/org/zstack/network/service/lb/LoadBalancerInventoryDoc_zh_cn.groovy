package org.zstack.network.service.lb

import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.network.service.lb.LoadBalancerListenerInventory

doc {

	title "在这里输入结构的名称"

	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
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
		name "state"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "4.0"
	}
	field {
		name "vipUuid"
		desc "VIP UUID"
		type "String"
		since "0.6"
	}
	field {
		name "serverGroupUuid"
		desc ""
		type "String"
		since "4.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.1"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.1"
	}
	ref {
		name "listeners"
		path "org.zstack.network.service.lb.LoadBalancerInventory.listeners"
		desc "null"
		type "List"
		since "0.6"
		clz LoadBalancerListenerInventory.class
	}
}
