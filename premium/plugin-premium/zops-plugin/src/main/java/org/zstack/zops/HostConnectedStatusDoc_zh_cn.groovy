package org.zstack.zops



doc {

	title "网络连通性状态"

	field {
		name "Connected"
		desc "网络可达"
		type "HostConnectedStatus"
		since "3.17.21"
	}
	field {
		name "Disconnected"
		desc "网络不可达"
		type "HostConnectedStatus"
		since "3.17.21"
	}
	field {
		name "Unknown"
		desc "网络连接状态未知"
		type "HostConnectedStatus"
		since "3.17.21"
	}
}
