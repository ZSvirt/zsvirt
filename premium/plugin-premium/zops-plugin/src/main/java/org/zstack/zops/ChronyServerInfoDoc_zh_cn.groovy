package org.zstack.zops

import org.zstack.zops.HostConnectedStatus

doc {

	title "chrony时间源服务器"

	field {
		name "hostname"
		desc "地址"
		type "String"
		since "3.17.21"
	}
	ref {
		name "status"
		path "org.zstack.zops.ChronyServerInfo.status"
		desc "连接状态"
		type "HostConnectedStatus"
		since "3.17.21"
		clz HostConnectedStatus.class
	}
}
