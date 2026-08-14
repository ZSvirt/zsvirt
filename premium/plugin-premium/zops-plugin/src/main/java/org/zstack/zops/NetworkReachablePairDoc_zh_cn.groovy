package org.zstack.zops

import org.zstack.zops.HostConnectedStatus

doc {

	title "网络连接状态对"

	field {
		name "sourceHostname"
		desc "源地址"
		type "String"
		since "3.17.21"
	}
	field {
		name "targetHostname"
		desc "目标地址"
		type "String"
		since "3.17.21"
	}
	ref {
		name "status"
		path "org.zstack.zops.NetworkReachablePair.status"
		desc "网络连接状态"
		type "HostConnectedStatus"
		since "3.17.21"
		clz HostConnectedStatus.class
	}
}
